package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.ui.components.AmountRow
import co.edu.udea.motoscaribe.autogestion.ui.components.EmptyState
import co.edu.udea.motoscaribe.autogestion.ui.components.Loading
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenColumn
import co.edu.udea.motoscaribe.autogestion.ui.components.SectionTitle
import co.edu.udea.motoscaribe.autogestion.ui.components.label
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.pretty

@Composable
fun FinesScreen(state: UiState) {
    val c = state.contract
    if (c == null) {
        Loading()
        return
    }
    ScreenColumn {
        Text(stringResource(R.string.fines_title), style = MaterialTheme.typography.titleLarge)
        if (state.fines.isEmpty()) {
            EmptyState(stringResource(R.string.no_fines))
        } else {
            state.fines.forEach {
                AmountRow(
                    it.reason.label(),
                    "${stringResource(R.string.fine_installment, it.installment)} · ${it.date.pretty()}",
                    it.amount.cop(),
                )
            }
        }
        SectionTitle(
            stringResource(
                R.string.fines_remaining,
                pluralStringResource(R.plurals.months, c.remainingMonths, c.remainingMonths),
            ),
        )
    }
}
