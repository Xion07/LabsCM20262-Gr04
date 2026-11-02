package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.ui.theme.MotosTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Urgencia de la cuota, derivada de los dias que faltan. */
enum class DueStatus { OK, SOON, TODAY, OVERDUE }

fun dueStatus(days: Int): DueStatus = when {
    days < 0 -> DueStatus.OVERDUE
    days == 0 -> DueStatus.TODAY
    days <= 3 -> DueStatus.SOON
    else -> DueStatus.OK
}

/** Porcentaje pagado del contrato, derivado de saldo/valor total (0..1). */
fun paidFraction(balance: Long, totalValue: Long): Float =
    if (totalValue <= 0L) 0f else (1f - balance.toFloat() / totalValue).coerceIn(0f, 1f)

/**
 * Todo lo que las pantallas necesitan para hablar de la proxima cuota con un mismo lenguaje:
 * dias, estado, texto largo ("Vence en 3 dias"), texto del chip ("Vence pronto") y color.
 */
@Immutable
data class DueInfo(
    val days: Int,
    val status: DueStatus,
    val text: String,
    val chip: String,
    /** Color sobre la tarjeta oscura (hero). */
    val heroColor: Color,
    /** Color sobre superficies claras/oscuras normales. */
    val color: Color,
    val container: Color,
    val onContainer: Color,
) {
    val urgent: Boolean get() = status == DueStatus.TODAY || status == DueStatus.OVERDUE
}

@Composable
fun rememberDueInfo(nextPaymentDate: LocalDate, today: LocalDate = LocalDate.now()): DueInfo {
    val days = ChronoUnit.DAYS.between(today, nextPaymentDate).toInt()
    val status = dueStatus(days)
    val text = when {
        days > 1 -> pluralStringResource(R.plurals.home_due_in, days, days)
        days == 1 -> stringResource(R.string.home_due_tomorrow)
        days == 0 -> stringResource(R.string.home_due_today)
        else -> pluralStringResource(R.plurals.home_overdue, -days, -days)
    }
    val chip = stringResource(
        when (status) {
            DueStatus.OK -> R.string.status_ok
            DueStatus.SOON -> R.string.status_soon
            DueStatus.TODAY -> R.string.status_today
            DueStatus.OVERDUE -> R.string.status_overdue
        }
    )
    val x = MotosTheme.colors
    val m = androidx.compose.material3.MaterialTheme.colorScheme
    return when (status) {
        DueStatus.OK -> DueInfo(days, status, text, chip, x.heroSuccess, x.success, x.successContainer, x.onSuccessContainer)
        DueStatus.SOON, DueStatus.TODAY -> DueInfo(days, status, text, chip, x.heroWarning, x.warning, x.warningContainer, x.onWarningContainer)
        DueStatus.OVERDUE -> DueInfo(days, status, text, chip, x.heroError, m.error, m.errorContainer, m.onErrorContainer)
    }
}
