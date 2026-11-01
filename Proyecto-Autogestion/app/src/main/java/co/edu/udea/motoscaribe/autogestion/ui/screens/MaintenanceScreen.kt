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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.domain.MaintenanceStatus
import co.edu.udea.motoscaribe.autogestion.ui.pretty

@Composable
fun MaintenanceScreen(state: UiState) {
    val c = state.contract ?: return
    val plan = state.maintenance
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Kilometraje", style = MaterialTheme.typography.titleLarge)

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text("Km recorridos desde el inicio del contrato", style = MaterialTheme.typography.labelMedium)
                Text("${c.kmTraveled} km", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }

        if (plan == null) {
            // Propuesta del proyecto: la BD aun no guarda el ultimo cambio de aceite.
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Próximamente: aviso de cambio de aceite", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Para avisarte cuándo toca el cambio hay que registrar la fecha del último " +
                            "cambio de aceite. Aún no se guarda; está propuesto para el sistema.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        } else {
            val container = when (plan.status) {
                MaintenanceStatus.OK -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.errorContainer
            }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(container)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cambio de aceite", style = MaterialTheme.typography.labelMedium)
                    Text(
                        when (plan.status) {
                            MaintenanceStatus.OVERDUE -> "Vencido por ${-plan.kmLeft} km"
                            else -> "Faltan ${plan.kmLeft} km"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    LinearProgressIndicator(progress = { plan.progress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "Actual ${plan.currentKm} km · próximo a los ${plan.nextServiceKm} km",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Text("Historial de kilometraje", style = MaterialTheme.typography.titleMedium)
        state.kmHistory.asReversed().forEach {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(it.date.pretty())
                Text("${it.km} km", fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider()
        }
    }
}
