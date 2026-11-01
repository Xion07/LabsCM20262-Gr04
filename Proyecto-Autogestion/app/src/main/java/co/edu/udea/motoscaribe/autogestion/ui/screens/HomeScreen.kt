package co.edu.udea.motoscaribe.autogestion.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import co.edu.udea.motoscaribe.autogestion.ui.components.Loading
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenColumn
import co.edu.udea.motoscaribe.autogestion.ui.components.SectionTitle
import co.edu.udea.motoscaribe.autogestion.ui.components.StatCard
import co.edu.udea.motoscaribe.autogestion.ui.components.label
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.pretty
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    val due = when {
        days > 1 -> pluralStringResource(R.plurals.home_due_in, days, days)
        days == 1 -> stringResource(R.string.home_due_tomorrow)
        days == 0 -> stringResource(R.string.home_due_today)
        else -> pluralStringResource(R.plurals.home_overdue, -days, -days)
    }

    ScreenColumn {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.home_greeting, c.holderName),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onLogout) { Text(stringResource(R.string.home_logout)) }
        }

        if (!alertsEnabled) {
            AlertCard(stringResource(R.string.home_alerts_off)) {
                TextButton({
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }) { Text(stringResource(R.string.home_alerts_enable)) }
            }
        }

        Card(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(Modifier.padding(16.dp)) {
                androidx.compose.foundation.layout.Column {
                    Text(
                        stringResource(R.string.home_next_payment),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        c.nextPaymentAmount.cop(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "$due · ${stringResource(R.string.home_due_on, c.nextPaymentDate.pretty())}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (days <= 0) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        Text(stringResource(R.string.home_cant_pay), style = MaterialTheme.typography.bodyMedium)
        FilledTonalButton(onRequestExtension, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.home_ask_extension))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.home_balance), c.balance.cop(), Modifier.weight(1f))
            StatCard(
                stringResource(R.string.home_term),
                pluralStringResource(R.plurals.months, c.remainingMonths, c.remainingMonths),
                Modifier.weight(1f),
            )
        }

        state.maintenance?.takeIf { it.status != MaintenanceStatus.OK }?.let { plan ->
            AlertCard(
                if (plan.kmLeft >= 0) stringResource(R.string.home_oil_soon, plan.kmLeft)
                else stringResource(R.string.home_oil_over, -plan.kmLeft)
            )
        }

        SectionTitle(stringResource(R.string.home_last_fines))
        if (state.fines.isEmpty()) {
            EmptyState(stringResource(R.string.no_fines))
        } else {
            state.fines.take(2).forEach {
                AmountRow(
                    it.reason.label(),
                    "${stringResource(R.string.fine_installment, it.installment)} · ${it.date.pretty()}",
                    it.amount.cop(),
                )
            }
        }
    }
}
