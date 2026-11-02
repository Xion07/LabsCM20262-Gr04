package co.edu.udea.motoscaribe.autogestion.data

import android.content.Context
import co.edu.udea.motoscaribe.autogestion.BuildConfig
import java.time.LocalDate

/**
 * Contrato de acceso a datos. [RemoteCreditRepository] habla con la API; [MockCreditRepository]
 * da datos de ejemplo (build con -PuseMock=true o sin URL de API).
 */
interface CreditRepository {
    /** Lanza [ApiException] (401 credenciales, 429 bloqueo) o [NetworkException]. */
    suspend fun login(document: String, password: String)
    fun logout()
    suspend fun contract(): Contract
    suspend fun payments(): List<Payment>
    suspend fun fines(): List<Fine>
    suspend fun kmHistory(): List<KmReading>
    suspend fun extensions(): List<ExtensionItem>
    suspend fun requestExtension(request: ExtensionRequest)
}

object RepositoryProvider {
    fun create(context: Context): CreditRepository =
        if (BuildConfig.API_BASE_URL.isBlank()) {
            MockCreditRepository()
        } else {
            RemoteCreditRepository(context.applicationContext, ApiClient(BuildConfig.API_BASE_URL))
        }
}

class MockCreditRepository : CreditRepository {
    private val today = LocalDate.now()

    override suspend fun login(document: String, password: String) {
        if (document.length < 6 || password.length < 4) throw ApiException(401)
    }

    override fun logout() = Unit

    override suspend fun contract() = Contract(
        holderName = "Camilo",
        plate = "ABC12D",
        totalValue = 6_800_000,
        balance = 2_450_000,
        nextPaymentDate = today.plusDays(5),
        nextPaymentAmount = 350_000,
        remainingMonths = 14,
        kmTraveled = 2_870,
        lastOilChangeKm = null, // aun no existe en la BD
    )

    override suspend fun payments() = listOf(
        Payment(today.minusDays(7), 350_000),
        Payment(today.minusDays(14), 350_000),
        Payment(today.minusDays(21), 350_000),
        Payment(today.minusDays(28), 350_000),
    )

    override suspend fun fines() = listOf(
        Fine(today.minusMonths(2), 25_000, FineReason.LATE_PAYMENT, 12),
        Fine(today.minusMonths(5), 25_000, FineReason.LATE_PAYMENT, 5),
    )

    override suspend fun kmHistory() = listOf(
        KmReading(today.minusDays(30), 2_050),
        KmReading(today.minusDays(20), 2_320),
        KmReading(today.minusDays(10), 2_610),
        KmReading(today, 2_870),
    )

    private val sent = mutableListOf<ExtensionItem>()

    override suspend fun extensions() = sent.reversed()

    override suspend fun requestExtension(request: ExtensionRequest) {
        sent += ExtensionItem(
            id = "mock-${sent.size}", dueDate = request.dueDate, newDate = request.newDate, excuse = request.excuse,
            hasAttachment = request.attachmentUri != null, status = RequestStatus.PENDING_REVIEW,
            createdAt = java.time.Instant.now(),
        )
    }
}
