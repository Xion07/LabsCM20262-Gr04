package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.domain.MaintenanceStatus
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.pretty

@Composable
fun HomeScreen(state: UiState) {
    val c = state.contract ?: return
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Hola, ${c.holderName}", style = MaterialTheme.typography.titleLarge)

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Saldo pendiente", style = MaterialTheme.typography.labelMedium)
                Text(
                    c.balance.cop(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard("Próximo pago", c.nextPaymentDate.pretty(), Modifier.weight(1f))
            InfoCard("Plazo restante", "${c.remainingMonths} meses", Modifier.weight(1f))
        }

        state.maintenance?.takeIf { it.status != MaintenanceStatus.OK }?.let { plan ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(
                    if (plan.kmLeft >= 0) "Cambio de aceite en ${plan.kmLeft} km"
                    else "Cambio de aceite vencido por ${-plan.kmLeft} km",
                    Modifier.padding(16.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Text("Últimas multas", style = MaterialTheme.typography.titleMedium)
        state.fines.take(2).forEach { FineRow(it.reason, it.installmentLabel, it.amount.cop()) }
    }
}

@Composable
private fun InfoCard(label: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun FineRow(reason: String, detail: String, amount: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(reason, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
            Text(amount, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
    }
}
