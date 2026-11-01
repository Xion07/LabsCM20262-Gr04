package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.pretty

@Composable
fun FinesScreen(state: UiState) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Multas", style = MaterialTheme.typography.titleLarge)
        state.fines.forEach {
            FineRow(it.reason, "${it.installmentLabel} · ${it.date.pretty()}", it.amount.cop())
        }
        state.contract?.let {
            Text(
                "Tiempo restante de pago: ${it.remainingMonths} meses",
                Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
