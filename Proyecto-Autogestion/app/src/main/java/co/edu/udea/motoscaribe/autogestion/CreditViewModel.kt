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
import co.edu.udea.motoscaribe.autogestion.data.ExtensionItem
import co.edu.udea.motoscaribe.autogestion.data.ExtensionRequest
import co.edu.udea.motoscaribe.autogestion.data.Fine
import co.edu.udea.motoscaribe.autogestion.data.KmReading
import co.edu.udea.motoscaribe.autogestion.data.NetworkException
import co.edu.udea.motoscaribe.autogestion.data.Payment
import co.edu.udea.motoscaribe.autogestion.data.PasskeyException
import co.edu.udea.motoscaribe.autogestion.data.PasskeyFailure
import co.edu.udea.motoscaribe.autogestion.data.RepositoryProvider
import co.edu.udea.motoscaribe.autogestion.domain.MaintenancePlan
import co.edu.udea.motoscaribe.autogestion.notifications.AlertScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginError { INVALID, LOCKED, NETWORK, SESSION_EXPIRED, PASSKEY_NONE, PASSKEY_FAILED, GENERIC }

/** Estado de la activacion del passkey en la pantalla de inicio. */
enum class PasskeyUi { IDLE, WORKING, ERROR }

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
    /** Recarga en curso (indicador de "deslizar para actualizar"). */
    val refreshing: Boolean = false,
    /** Solicitudes de plazo ya enviadas, la mas reciente primero. */
    val requests: List<ExtensionItem> = emptyList(),
    val submit: SubmitState = SubmitState.IDLE,
    /** La API soporta passkeys (no aplica al mock de ejemplo). */
    val passkeysAvailable: Boolean = false,
    /** null mientras no se sabe; true si ya hay un passkey registrado para esta cuenta. */
    val passkeyRegistered: Boolean? = null,
    val passkeyUi: PasskeyUi = PasskeyUi.IDLE,
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

    private fun fresh(reason: LoginError? = null) =
        UiState(loginError = reason, passkeysAvailable = repository.passkeysAvailable)

    private val _state = MutableStateFlow(fresh())
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

    /** Ingreso con passkey: [sign] abre el dialogo del sistema (huella, rostro o PIN) y firma el reto. */
    fun loginWithPasskey(sign: suspend (optionsJson: String) -> String) {
        if (_state.value.loggingIn) return
        viewModelScope.launch {
            _state.update { it.copy(loggingIn = true, loginError = null) }
            try {
                repository.loginWithPasskey(sign)
                _state.update { it.copy(loggedIn = true, loggingIn = false, passkeyRegistered = true) }
                load()
            } catch (e: PasskeyException) {
                // Cerrar el dialogo no es un error: se vuelve al formulario sin mensaje.
                val error = when (e.kind) {
                    PasskeyFailure.CANCELLED -> null
                    PasskeyFailure.NONE -> LoginError.PASSKEY_NONE
                    PasskeyFailure.FAILED -> LoginError.PASSKEY_FAILED
                }
                _state.update { it.copy(loggingIn = false, loginError = error) }
            } catch (e: Exception) {
                val error = if (e is ApiException && e.code == 401) LoginError.PASSKEY_FAILED else e.toLoginError()
                _state.update { it.copy(loggingIn = false, loginError = error) }
            }
        }
    }

    /** Crea un passkey para esta cuenta (con la sesion iniciada con clave). */
    fun registerPasskey(create: suspend (optionsJson: String) -> String) {
        if (_state.value.passkeyUi == PasskeyUi.WORKING) return
        viewModelScope.launch {
            _state.update { it.copy(passkeyUi = PasskeyUi.WORKING) }
            try {
                repository.registerPasskey(create)
                _state.update { it.copy(passkeyUi = PasskeyUi.IDLE, passkeyRegistered = true) }
            } catch (e: PasskeyException) {
                val ui = if (e.kind == PasskeyFailure.CANCELLED) PasskeyUi.IDLE else PasskeyUi.ERROR
                _state.update { it.copy(passkeyUi = ui) }
            } catch (e: Exception) {
                if (e is ApiException && e.code == 401) endSession(LoginError.SESSION_EXPIRED)
                else _state.update { it.copy(passkeyUi = PasskeyUi.ERROR) }
            }
        }
    }

    fun removePasskeys() {
        viewModelScope.launch {
            try {
                repository.removePasskeys()
                _state.update { it.copy(passkeyRegistered = false, passkeyUi = PasskeyUi.IDLE) }
            } catch (e: Exception) {
                if (e is ApiException && e.code == 401) endSession(LoginError.SESSION_EXPIRED)
            }
        }
    }

    /** Saber si ya hay passkey es secundario: si falla, la tarjeta simplemente no aparece. */
    private suspend fun loadPasskeyStatus() {
        if (!repository.passkeysAvailable) return
        try {
            val registered = repository.hasPasskey()
            _state.update { it.copy(passkeyRegistered = registered) }
        } catch (e: Exception) {
            if (e is ApiException && e.code == 401) endSession(LoginError.SESSION_EXPIRED)
        }
    }

    /** Carga (o recarga) todos los datos del usuario. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loadError = false, refreshing = true) }
            try {
                val contract = repository.contract()
                val payments = repository.payments()
                val fines = repository.fines()
                val km = repository.kmHistory()
                _state.update {
                    it.copy(contract = contract, payments = payments, fines = fines, kmHistory = km, refreshing = false)
                }
                loadRequests()
                loadPasskeyStatus()
                onContractLoaded(contract)
            } catch (e: Exception) {
                if (e is ApiException && e.code == 401) endSession(LoginError.SESSION_EXPIRED)
                else _state.update { it.copy(loadError = true, refreshing = false) }
            }
        }
    }

    /** El historial de solicitudes es secundario: si falla, no se bloquea el resto de la app. */
    private suspend fun loadRequests() {
        try {
            val list = repository.extensions()
            _state.update { it.copy(requests = list) }
        } catch (e: Exception) {
            if (e is ApiException && e.code == 401) endSession(LoginError.SESSION_EXPIRED)
        }
    }

    fun logout() = endSession(null)

    private fun endSession(reason: LoginError?) {
        repository.logout()
        onSessionEnded()
        _state.value = fresh(reason)
    }

    fun submitExtension(request: ExtensionRequest) {
        if (_state.value.submit == SubmitState.SENDING) return
        viewModelScope.launch {
            _state.update { it.copy(submit = SubmitState.SENDING) }
            try {
                repository.requestExtension(request)
                _state.update { it.copy(submit = SubmitState.SUCCESS) }
                loadRequests()
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
