package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.ui.components.AmountRow
import co.edu.udea.motoscaribe.autogestion.ui.components.EmptyState
import co.edu.udea.motoscaribe.autogestion.ui.components.HeroCard
import co.edu.udea.motoscaribe.autogestion.ui.components.Loading
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenColumn
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenHeader
import co.edu.udea.motoscaribe.autogestion.ui.components.StatCard
import co.edu.udea.motoscaribe.autogestion.ui.components.label
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.motion.enterFromBelow
import co.edu.udea.motoscaribe.autogestion.ui.motion.rememberCountingValue
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
    ScreenColumn {
        ScreenHeader(stringResource(R.string.fines_title), stringResource(R.string.fines_subtitle), Modifier.enterFromBelow(0))

        if (state.fines.isEmpty()) {
            EmptyState(stringResource(R.string.no_fines), Icons.Rounded.Verified, Modifier.enterFromBelow(1))
        } else {
            HeroCard(Modifier.enterFromBelow(1), glowColor = x.heroError) {
                Text(stringResource(R.string.fines_total), style = MaterialTheme.typography.labelLarge, color = x.onHeroMuted)
                val shown by rememberCountingValue(total)
                Text(shown.cop(), style = MaterialTheme.typography.displaySmall, color = x.onHero)
                Text(
                    pluralStringResource(R.plurals.fines_count, state.fines.size, state.fines.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = x.heroError,
                )
            }
            state.fines.forEachIndexed { i, it ->
                AmountRow(
                    it.reason.label(),
                    "${stringResource(R.string.fine_installment, it.installment)} · ${it.date.pretty()}",
                    it.amount.cop(),
                    Icons.Rounded.Gavel,
                    Modifier.enterFromBelow(2 + i.coerceAtMost(6)),
                )
            }
        }

        Row(Modifier.fillMaxWidth().enterFromBelow(3 + state.fines.size.coerceAtMost(6)), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                stringResource(R.string.fines_remaining_label),
                pluralStringResource(R.plurals.months, c.remainingMonths, c.remainingMonths),
                Icons.Rounded.CalendarMonth,
                Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
