package co.edu.udea.motoscaribe.autogestion.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class AlertLevel { UPCOMING, DUE_TODAY, OVERDUE }

/** Decision de alerta sin texto: el texto sale de los recursos segun el idioma. */
data class PaymentAlert(
    val level: AlertLevel,
    /** Dias que faltan (UPCOMING) o dias de mora (OVERDUE); 0 si vence hoy. */
    val days: Int,
    /** Las alertas de vencimiento y mora usan un canal de alta prioridad. */
    val urgent: Boolean,
    /** Horas hasta la siguiente revision: se acorta mientras el pago no se registre. */
    val nextCheckHours: Long,
)

object AlertPolicy {
    const val IDLE_CHECK_HOURS = 12L

    /** Devuelve la alerta que corresponde hoy, o null si aun falta mas de 3 dias. */
    fun evaluate(today: LocalDate, dueDate: LocalDate): PaymentAlert? {
        val days = ChronoUnit.DAYS.between(today, dueDate).toInt()
        return when {
            days > 3 -> null
            days >= 1 -> PaymentAlert(AlertLevel.UPCOMING, days, urgent = false, nextCheckHours = 24)
            days == 0 -> PaymentAlert(AlertLevel.DUE_TODAY, 0, urgent = true, nextCheckHours = 6)
            else -> PaymentAlert(AlertLevel.OVERDUE, -days, urgent = true, nextCheckHours = 3)
        }
    }
}
