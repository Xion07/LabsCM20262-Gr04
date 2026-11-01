package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.ui.components.ContentMaxWidth
import co.edu.udea.motoscaribe.autogestion.ui.components.EmptyState
import co.edu.udea.motoscaribe.autogestion.ui.components.Loading
import co.edu.udea.motoscaribe.autogestion.ui.components.SectionTitle
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.pretty

@Composable
fun BalanceScreen(state: UiState) {
    val c = state.contract
    if (c == null) {
        Loading()
        return
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.balance_total),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(c.totalValue.cop(), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.balance_km, c.kmTraveled),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item { SectionTitle(stringResource(R.string.balance_history), Modifier.padding(top = 8.dp)) }
            if (state.payments.isEmpty()) {
                item { EmptyState(stringResource(R.string.balance_empty)) }
            } else {
                items(state.payments) { p ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(p.date.pretty())
                        Text(p.amount.cop(), fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
