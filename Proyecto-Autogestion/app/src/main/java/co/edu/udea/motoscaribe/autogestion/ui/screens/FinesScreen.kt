package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.ui.components.Adaptive
import co.edu.udea.motoscaribe.autogestion.ui.components.AmountRowChip
import co.edu.udea.motoscaribe.autogestion.ui.components.EmptyState
import co.edu.udea.motoscaribe.autogestion.ui.components.HeroCard
import co.edu.udea.motoscaribe.autogestion.ui.components.InfoChip
import co.edu.udea.motoscaribe.autogestion.ui.components.Loading
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenColumn
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenHeader
import co.edu.udea.motoscaribe.autogestion.ui.components.SectionTitle
import co.edu.udea.motoscaribe.autogestion.ui.components.StatCard
import co.edu.udea.motoscaribe.autogestion.ui.components.StatRow
import co.edu.udea.motoscaribe.autogestion.ui.components.StatusChip
import co.edu.udea.motoscaribe.autogestion.ui.components.label
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.motion.EmptyKind
import co.edu.udea.motoscaribe.autogestion.ui.motion.enterFromBelow
import co.edu.udea.motoscaribe.autogestion.ui.motion.rememberCountingValue
import co.edu.udea.motoscaribe.autogestion.ui.motion.rememberPulse
import co.edu.udea.motoscaribe.autogestion.ui.pretty
import co.edu.udea.motoscaribe.autogestion.ui.theme.MotosTheme

@Composable
fun FinesScreen(state: UiState) {
    val c = state.contract
    if (c == null) {
        Loading()
        return
    }
    val x = MotosTheme.colors
    val total = state.fines.sumOf { it.amount }
    val due = rememberDueInfo(c.nextPaymentDate)
    val pulse by rememberPulse(due.urgent)
    val hasFines = state.fines.isNotEmpty()
    ScreenColumn {
        ScreenHeader(stringResource(R.string.fines_title), stringResource(R.string.fines_subtitle), Modifier.enterFromBelow(0))

        Adaptive(
            primary = {
                // Resumen: total, cuantas multas y en que estado va la cuota actual.
                HeroCard(Modifier.enterFromBelow(1), glowColor = if (hasFines) x.heroError else x.heroSuccess) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.fines_total),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            color = x.onHeroMuted,
                        )
                        StatusChip(due.chip, due.heroColor, Color.White.copy(alpha = 0.12f), x.onHero, glow = pulse)
                    }
                    val shown by rememberCountingValue(total)
                    Text(shown.cop(), style = MaterialTheme.typography.displaySmall, color = x.onHero)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(
                            if (hasFines) pluralStringResource(R.plurals.fines_count, state.fines.size, state.fines.size)
                            else stringResource(R.string.fines_none_short),
                            icon = Icons.Rounded.Gavel,
                            color = if (hasFines) x.heroError else x.heroSuccess,
                            container = Color.White.copy(alpha = 0.10f),
                        )
                        InfoChip(
                            due.text,
                            icon = Icons.Rounded.Schedule,
                            color = x.onHero,
                            container = Color.White.copy(alpha = 0.10f),
                        )
                    }
                }

                StatRow(Modifier.enterFromBelow(2)) {
                    StatCard(
                        stringResource(R.string.fines_next_payment),
                        c.nextPaymentAmount.cop(),
                        Icons.Rounded.Schedule,
                        Modifier.weight(1f),
                        accent = due.color,
                    )
                    StatCard(
                        stringResource(R.string.home_term),
                        pluralStringResource(R.plurals.months, c.remainingMonths, c.remainingMonths),
                        Icons.Rounded.CalendarMonth,
                        Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.tertiary,
                    )
                }

                // Consejo breve: por que aparecen las multas y como evitarlas.
                Card(
                    Modifier.fillMaxWidth().enterFromBelow(3).semantics(mergeDescendants = true) {},
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Lightbulb, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                        Text(
                            stringResource(R.string.fines_tip),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            secondary = {
                SectionTitle(stringResource(R.string.fines_detail), Modifier.enterFromBelow(3))
                if (!hasFines) {
                    EmptyState(stringResource(R.string.no_fines), stringResource(R.string.no_fines_body), EmptyKind.FINES, Modifier.enterFromBelow(4))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.fines.forEachIndexed { i, it ->
                            AmountRowChip(
                                it.reason.label(),
                                stringResource(R.string.fine_installment, it.installment),
                                it.date.pretty(),
                                it.amount.cop(),
                                Icons.Rounded.Gavel,
                                Modifier.enterFromBelow(4 + i.coerceAtMost(6)),
                            )
                        }
                    }
                }
            },
        )
    }
}
