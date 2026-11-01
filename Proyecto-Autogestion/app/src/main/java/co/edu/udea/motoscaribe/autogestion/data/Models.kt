package co.edu.udea.motoscaribe.autogestion.data

import java.time.LocalDate

/** Estado del contrato de arrendamiento del usuario. Montos en pesos COP. */
data class Contract(
    val holderName: String,
    val plate: String,
    val totalValue: Long,
    val balance: Long,
    val nextPaymentDate: LocalDate,
    val nextPaymentAmount: Long,
    val remainingMonths: Int,
    /** Km recorridos por la moto desde el inicio del contrato (suma de hist_distancia). */
    val kmTraveled: Int,
    /**
     * Km en que se hizo el ultimo cambio de aceite. Hoy NO existe en la BD: es una
     * propuesta del proyecto. Mientras sea null la app solo muestra los km andados.
     */
    val lastOilChangeKm: Int?,
)

data class Payment(val date: LocalDate, val amount: Long)

/** Motivo de la multa; la UI lo traduce a texto segun el idioma del sistema. */
enum class FineReason { LATE_PAYMENT, OTHER }

data class Fine(
    val date: LocalDate,
    val amount: Long,
    val reason: FineReason,
    val installment: Int,
)

data class KmReading(val date: LocalDate, val km: Int)

enum class RequestStatus { PENDING_REVIEW }

/** Solicitud del arrendatario: ampliacion de plazo (newDate) y/o excusa con soporte. */
data class ExtensionRequest(
    val dueDate: LocalDate,
    val newDate: LocalDate?,
    val excuse: String,
    val attachmentUri: String? = null,
    val status: RequestStatus = RequestStatus.PENDING_REVIEW,
)
