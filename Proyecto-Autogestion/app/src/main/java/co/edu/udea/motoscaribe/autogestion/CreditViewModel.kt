package co.edu.udea.motoscaribe.autogestion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.edu.udea.motoscaribe.autogestion.data.AlertCache
import co.edu.udea.motoscaribe.autogestion.data.ApiException
import co.edu.udea.motoscaribe.autogestion.data.Contract
import co.edu.udea.motoscaribe.autogestion.data.CreditRepository
import co.edu.udea.motoscaribe.autogestion.data.ExtensionRequest
import co.edu.udea.motoscaribe.autogestion.data.Fine
import co.edu.udea.motoscaribe.autogestion.data.KmReading
import co.edu.udea.motoscaribe.autogestion.data.NetworkException
import co.edu.udea.motoscaribe.autogestion.data.Payment
import co.edu.udea.motoscaribe.autogestion.data.RepositoryProvider
import co.edu.udea.motoscaribe.autogestion.domain.MaintenancePlan
import co.edu.udea.motoscaribe.autogestion.notifications.AlertScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginError { INVALID, LOCKED, NETWORK, SESSION_EXPIRED, GENERIC }

enum class SubmitState { IDLE, SENDING, SUCCESS, ERROR }

data class UiState(
    val loggedIn: Boolean = false,
    val loggingIn: Boolean = false,
    val loginError: LoginError? = null,
    val contract: Contract? = null,
    val payments: List<Payment> = emptyList(),
    val fines: List<Fine> = emptyList(),
    val kmHistory: List<KmReading> = emptyList(),
    /** Fallo al cargar los datos tras ingresar (se ofrece reintentar). */
    val loadError: Boolean = false,
    val lastRequest: ExtensionRequest? = null,
    val submit: SubmitState = SubmitState.IDLE,
) {
    /** Solo hay plan si se conoce el ultimo cambio de aceite (propuesta, no existe en BD). */
    val maintenance: MaintenancePlan?
        get() = contract?.let { c -> c.lastOilChangeKm?.let { MaintenancePlan(c.kmTraveled, it) } }
}

class CreditViewModel(
    private val repository: CreditRepository,
    /** Se llama al cargar el contrato (guarda la copia para las alertas). */
    private val onContractLoaded: (Contract) -> Unit = {},
    private val onSessionEnded: () -> Unit = {},
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun login(document: String, password: String) {
        if (_state.value.loggingIn) return
        viewModelScope.launch {
            _state.update { it.copy(loggingIn = true, loginError = null) }
            try {
                repository.login(document, password)
                _state.update { it.copy(loggedIn = true, loggingIn = false) }
                load()
            } catch (e: Exception) {
                _state.update { it.copy(loggingIn = false, loginError = e.toLoginError()) }
            }
        }
    }

    /** Carga (o recarga) todos los datos del usuario. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loadError = false) }
            try {
                val contract = repository.contract()
                val payments = repository.payments()
                val fines = repository.fines()
                val km = repository.kmHistory()
                _state.update {
                    it.copy(contract = contract, payments = payments, fines = fines, kmHistory = km)
                }
                onContractLoaded(contract)
            } catch (e: Exception) {
                if (e is ApiException && e.code == 401) endSession(LoginError.SESSION_EXPIRED)
                else _state.update { it.copy(loadError = true) }
            }
        }
    }

    fun logout() = endSession(null)

    private fun endSession(reason: LoginError?) {
        repository.logout()
        onSessionEnded()
        _state.value = UiState(loginError = reason)
    }

    fun submitExtension(request: ExtensionRequest) {
        if (_state.value.submit == SubmitState.SENDING) return
        viewModelScope.launch {
            _state.update { it.copy(submit = SubmitState.SENDING) }
            try {
                repository.requestExtension(request)
                _state.update { it.copy(submit = SubmitState.SUCCESS, lastRequest = request) }
            } catch (e: Exception) {
                if (e is ApiException && e.code == 401) endSession(LoginError.SESSION_EXPIRED)
                else _state.update { it.copy(submit = SubmitState.ERROR) }
            }
        }
    }

    /** La pantalla ya mostro el resultado del envio. */
    fun submitConsumed() = _state.update { it.copy(submit = SubmitState.IDLE) }

    private fun Exception.toLoginError(): LoginError = when {
        this is ApiException && code == 401 -> LoginError.INVALID
        this is ApiException && code == 429 -> LoginError.LOCKED
        this is NetworkException -> LoginError.NETWORK
        else -> LoginError.GENERIC
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                val app = context.applicationContext
                CreditViewModel(
                    repository = RepositoryProvider.create(app),
                    onContractLoaded = {
                        AlertCache.save(app, it)
                        AlertScheduler.restart(app) // evalua ya con los datos recien llegados
                    },
                    onSessionEnded = {
                        AlertCache.clear(app)
                        AlertScheduler.cancel(app)
                    },
                )
            }
        }
    }
}
