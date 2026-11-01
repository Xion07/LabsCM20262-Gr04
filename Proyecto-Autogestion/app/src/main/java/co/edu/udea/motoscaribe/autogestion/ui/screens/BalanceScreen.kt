package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.pretty

@Composable
fun BalanceScreen(state: UiState) {
    val c = state.contract ?: return
    LazyColumn(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total moto", style = MaterialTheme.typography.labelMedium)
                    Text(c.totalValue.cop(), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Km recorridos: ${c.kmTraveled} km",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                "Historial de abonos",
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(state.payments) { p ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(p.date.pretty())
                Text(p.amount.cop(), fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider()
        }
    }
}
