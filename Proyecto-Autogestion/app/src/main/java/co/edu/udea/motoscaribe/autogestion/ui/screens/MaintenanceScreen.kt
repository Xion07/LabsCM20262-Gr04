package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.domain.MaintenanceStatus
import co.edu.udea.motoscaribe.autogestion.ui.components.Loading
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenColumn
import co.edu.udea.motoscaribe.autogestion.ui.components.SectionTitle
import co.edu.udea.motoscaribe.autogestion.ui.pretty

@Composable
fun MaintenanceScreen(state: UiState) {
    val c = state.contract
    if (c == null) {
        Loading()
        return
    }
    val plan = state.maintenance
    ScreenColumn {
        Text(stringResource(R.string.km_title), style = MaterialTheme.typography.titleLarge)

        Card(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.km_traveled_label), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.km_format, c.kmTraveled),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (plan == null) {
            // El ultimo cambio de aceite aun no se guarda en la BD (propuesta del proyecto).
            Card(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.km_oil_coming), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.km_oil_coming_body), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            val container = if (plan.status == MaintenanceStatus.OK) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
            Card(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                colors = CardDefaults.cardColors(container),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.km_oil_title), style = MaterialTheme.typography.labelLarge)
                    Text(
                        if (plan.status == MaintenanceStatus.OVERDUE) stringResource(R.string.km_over, -plan.kmLeft)
                        else stringResource(R.string.km_left, plan.kmLeft),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    LinearProgressIndicator(progress = { plan.progress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        stringResource(R.string.km_progress, plan.currentKm, plan.nextServiceKm),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        SectionTitle(stringResource(R.string.km_history))
        state.kmHistory.asReversed().forEach {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(it.date.pretty())
                Text(stringResource(R.string.km_format, it.km), fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider()
        }
    }
}
