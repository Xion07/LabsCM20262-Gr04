package co.edu.udea.motoscaribe.autogestion.data

import java.time.LocalDate

/**
 * Contrato de acceso a datos. Hoy lo implementa [MockCreditRepository]; cuando
 * exista la API intermedia se agrega una implementacion remota sin tocar la UI.
 */
interface CreditRepository {
    suspend fun contract(): Contract
    suspend fun payments(): List<Payment>
    suspend fun fines(): List<Fine>
    suspend fun kmHistory(): List<KmReading>
    suspend fun requestExtension(request: ExtensionRequest)
}

class MockCreditRepository : CreditRepository {
    private val today = LocalDate.now()

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

    override suspend fun requestExtension(request: ExtensionRequest) {
        // Mock: sin persistencia.
    }
}
