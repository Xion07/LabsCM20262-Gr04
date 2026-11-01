package co.edu.udea.motoscaribe.autogestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.motoscaribe.autogestion.data.Contract
import co.edu.udea.motoscaribe.autogestion.data.CreditRepository
import co.edu.udea.motoscaribe.autogestion.data.ExtensionRequest
import co.edu.udea.motoscaribe.autogestion.data.Fine
import co.edu.udea.motoscaribe.autogestion.data.KmReading
import co.edu.udea.motoscaribe.autogestion.data.MockCreditRepository
import co.edu.udea.motoscaribe.autogestion.data.Payment
import co.edu.udea.motoscaribe.autogestion.domain.MaintenancePlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val loggedIn: Boolean = false,
    val contract: Contract? = null,
    val payments: List<Payment> = emptyList(),
    val fines: List<Fine> = emptyList(),
    val kmHistory: List<KmReading> = emptyList(),
    val lastRequest: ExtensionRequest? = null,
) {
    /** Solo hay plan si se conoce el ultimo cambio de aceite (propuesta, no existe en BD). */
    val maintenance: MaintenancePlan?
        get() = contract?.let { c -> c.lastOilChangeKm?.let { MaintenancePlan(c.kmTraveled, it) } }
}

class CreditViewModel(
    private val repository: CreditRepository = MockCreditRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /**
     * Login simulado: cedula (6+ digitos) y clave (4+ caracteres).
     * Se reemplaza por la autenticacion de la API cuando exista.
     */
    fun login(document: String, password: String): Boolean {
        if (document.length < 6 || password.length < 4) return false
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loggedIn = true,
                    contract = repository.contract(),
                    payments = repository.payments(),
                    fines = repository.fines(),
                    kmHistory = repository.kmHistory(),
                )
            }
        }
        return true
    }

    fun logout() = _state.update { UiState() }

    fun submitExtension(request: ExtensionRequest) {
        viewModelScope.launch {
            repository.requestExtension(request)
            _state.update { it.copy(lastRequest = request) }
        }
    }
}
