package co.edu.udea.motoscaribe.autogestion.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.OilBarrel
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.domain.MaintenanceStatus
import co.edu.udea.motoscaribe.autogestion.ui.components.AlertCard
import co.edu.udea.motoscaribe.autogestion.ui.components.AmountRow
import co.edu.udea.motoscaribe.autogestion.ui.components.EmptyState
import co.edu.udea.motoscaribe.autogestion.ui.components.HeroCard
import co.edu.udea.motoscaribe.autogestion.ui.components.Loading
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenColumn
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenHeader
import co.edu.udea.motoscaribe.autogestion.ui.components.SectionTitle
import co.edu.udea.motoscaribe.autogestion.ui.components.StatCard
import co.edu.udea.motoscaribe.autogestion.ui.components.StatusChip
import co.edu.udea.motoscaribe.autogestion.ui.components.label
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.motion.ProgressRing
import co.edu.udea.motoscaribe.autogestion.ui.motion.enterFromBelow
import co.edu.udea.motoscaribe.autogestion.ui.motion.pressFeedback
import co.edu.udea.motoscaribe.autogestion.ui.motion.rememberCountingValue
import co.edu.udea.motoscaribe.autogestion.ui.motion.rememberPulse
import co.edu.udea.motoscaribe.autogestion.ui.pretty
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

@Composable
fun HomeScreen(state: UiState, onRequestExtension: () -> Unit, onLogout: () -> Unit) {
    val c = state.contract
    if (c == null) {
        Loading()
        return
    }
    val context = LocalContext.current
    val alertsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val days = ChronoUnit.DAYS.between(LocalDate.now(), c.nextPaymentDate).toInt()
    val status = dueStatus(days)
    val due = when {
        days > 1 -> pluralStringResource(R.plurals.home_due_in, days, days)
        days == 1 -> stringResource(R.string.home_due_tomorrow)
        days == 0 -> stringResource(R.string.home_due_today)
        else -> pluralStringResource(R.plurals.home_overdue, -days, -days)
    }
    val x = MotosTheme.colors
    val statusColor = when (status) {
        DueStatus.OK -> x.heroSuccess
        DueStatus.SOON, DueStatus.TODAY -> x.heroWarning
        DueStatus.OVERDUE -> x.heroError
    }
    val statusText = stringResource(
        when (status) {
            DueStatus.OK -> R.string.status_ok
            DueStatus.SOON -> R.string.status_soon
            DueStatus.TODAY -> R.string.status_today
            DueStatus.OVERDUE -> R.string.status_overdue
        }
    )
    val pulse by rememberPulse(status == DueStatus.TODAY || status == DueStatus.OVERDUE)

    ScreenColumn {
        ScreenHeader(
            stringResource(R.string.home_greeting, c.holderName),
            stringResource(R.string.home_plate, c.plate),
            Modifier.enterFromBelow(0),
        ) {
            IconButton(onLogout, Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Logout, stringResource(R.string.home_logout), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (!alertsEnabled) {
            AlertCard(stringResource(R.string.home_alerts_off), Modifier.enterFromBelow(1), Icons.Rounded.NotificationsOff) {
                TextButton({
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }) { Text(stringResource(R.string.home_alerts_enable)) }
            }
        }

        // Tarjeta protagonista: la cuota cuenta hasta su valor y el estado tiene halo y pulso.
        HeroCard(Modifier.enterFromBelow(1), glowColor = statusColor) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.home_next_payment),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = x.onHeroMuted,
                )
                StatusChip(statusText, statusColor, Color.White.copy(alpha = 0.12f), x.onHero, glow = pulse)
            }
            val amount by rememberCountingValue(c.nextPaymentAmount)
            Text(amount.cop(), style = MaterialTheme.typography.displayMedium, color = x.onHero)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(16.dp), tint = statusColor)
                Text(
                    "$due · ${stringResource(R.string.home_due_on, c.nextPaymentDate.pretty())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (days <= 0) statusColor else x.onHeroMuted,
                    fontWeight = if (days <= 0) FontWeight.Bold else FontWeight.Normal,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.home_cant_pay), style = MaterialTheme.typography.bodySmall, color = x.onHeroMuted)
            val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Button(
                onRequestExtension,
                Modifier.fillMaxWidth().heightIn(min = 50.dp).pressFeedback(interaction),
                interactionSource = interaction,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = x.heroStart),
            ) {
                Icon(Icons.Rounded.EventRepeat, null, Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.home_ask_extension), style = MaterialTheme.typography.labelLarge)
            }
        }

        // Avance: anillo que se dibuja hasta el porcentaje pagado (derivado de saldo / valor total).
        val fraction = paidFraction(c.balance, c.totalValue)
        Card(
            Modifier.fillMaxWidth().enterFromBelow(2).semantics(mergeDescendants = true) {},
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                val pct by rememberCountingValue((fraction * 100).toLong())
                ProgressRing(fraction, size = 104.dp) {
                    Text("$pct%", style = MaterialTheme.typography.titleLarge)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.home_progress_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.home_progress_body, (c.totalValue - c.balance).coerceAtLeast(0).cop(), c.totalValue.cop()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(Modifier.enterFromBelow(3), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val balance by rememberCountingValue(c.balance)
            StatCard(stringResource(R.string.home_balance), balance.cop(), Icons.Rounded.AccountBalanceWallet, Modifier.weight(1f))
            StatCard(
                stringResource(R.string.home_term),
                pluralStringResource(R.plurals.months, c.remainingMonths, c.remainingMonths),
                Icons.Rounded.CalendarMonth,
                Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.tertiary,
            )
        }

        state.maintenance?.takeIf { it.status != MaintenanceStatus.OK }?.let { plan ->
            AlertCard(
                if (plan.kmLeft >= 0) stringResource(R.string.home_oil_soon, plan.kmLeft)
                else stringResource(R.string.home_oil_over, -plan.kmLeft),
                Modifier.enterFromBelow(4),
                Icons.Rounded.OilBarrel,
            )
        }

        SectionTitle(stringResource(R.string.home_last_fines), Modifier.enterFromBelow(4))
        if (state.fines.isEmpty()) {
            EmptyState(stringResource(R.string.no_fines), Icons.Rounded.Verified, Modifier.enterFromBelow(5))
        } else {
            state.fines.take(2).forEachIndexed { i, it ->
                AmountRow(
                    it.reason.label(),
                    "${stringResource(R.string.fine_installment, it.installment)} · ${it.date.pretty()}",
                    it.amount.cop(),
                    Icons.Rounded.Gavel,
                    Modifier.enterFromBelow(5 + i),
                )
            }
        }
    }
}
