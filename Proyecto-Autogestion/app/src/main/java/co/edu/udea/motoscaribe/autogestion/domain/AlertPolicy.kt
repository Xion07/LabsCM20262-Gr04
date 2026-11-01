package co.edu.udea.motoscaribe.autogestion.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class AlertLevel { UPCOMING, DUE_TODAY, OVERDUE }

data class PaymentAlert(
    val level: AlertLevel,
    val title: String,
    val message: String,
    /** Las alertas de vencimiento y mora usan un canal de alta prioridad. */
    val urgent: Boolean,
    /** Horas hasta la siguiente revision: se acorta mientras el pago no se registre. */
    val nextCheckHours: Long,
)

object AlertPolicy {
    const val IDLE_CHECK_HOURS = 12L

    /** Devuelve la alerta que corresponde hoy, o null si aun falta mas de 3 dias. */
    fun evaluate(today: LocalDate, dueDate: LocalDate, amountLabel: String): PaymentAlert? {
        val days = ChronoUnit.DAYS.between(today, dueDate)
        return when {
            days > 3 -> null
            days >= 1 -> PaymentAlert(
                AlertLevel.UPCOMING,
                "Tu cuota vence pronto",
                "Faltan $days ${if (days == 1L) "día" else "días"} para pagar $amountLabel.",
                urgent = false,
                nextCheckHours = 24,
            )
            days == 0L -> PaymentAlert(
                AlertLevel.DUE_TODAY,
                "Tu cuota vence hoy",
                "Paga $amountLabel hoy para evitar multa. Si no puedes, solicita ampliación de plazo.",
                urgent = true,
                nextCheckHours = 6,
            )
            else -> {
                val late = -days
                PaymentAlert(
                    AlertLevel.OVERDUE,
                    "Cuota en mora",
                    "Llevas $late ${if (late == 1L) "día" else "días"} de mora ($amountLabel). " +
                        "Paga o envía una excusa desde la app.",
                    urgent = true,
                    nextCheckHours = 3,
                )
            }
        }
    }
}
