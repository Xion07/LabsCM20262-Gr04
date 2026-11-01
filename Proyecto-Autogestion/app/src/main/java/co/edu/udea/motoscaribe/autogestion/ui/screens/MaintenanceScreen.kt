package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OilBarrel
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Speed
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.domain.MaintenanceStatus
import co.edu.udea.motoscaribe.autogestion.ui.components.EmptyState
import co.edu.udea.motoscaribe.autogestion.ui.components.HeroCard
import co.edu.udea.motoscaribe.autogestion.ui.components.Loading
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenColumn
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenHeader
import co.edu.udea.motoscaribe.autogestion.ui.components.SectionTitle
import co.edu.udea.motoscaribe.autogestion.ui.components.TimelineRow
import co.edu.udea.motoscaribe.autogestion.ui.motion.KmSparkline
import co.edu.udea.motoscaribe.autogestion.ui.motion.MotoRider
import co.edu.udea.motoscaribe.autogestion.ui.motion.enterFromBelow
import co.edu.udea.motoscaribe.autogestion.ui.motion.rememberCountingValue
import co.edu.udea.motoscaribe.autogestion.ui.motion.rememberDrawnProgress
import co.edu.udea.motoscaribe.autogestion.ui.pretty
import co.edu.udea.motoscaribe.autogestion.ui.theme.BrandRed
import co.edu.udea.motoscaribe.autogestion.ui.theme.MotosTheme

@Composable
fun MaintenanceScreen(state: UiState) {
    val c = state.contract
    if (c == null) {
        Loading()
        return
    }
    val plan = state.maintenance
    val x = MotosTheme.colors
    ScreenColumn {
        ScreenHeader(stringResource(R.string.km_title), stringResource(R.string.km_subtitle), Modifier.enterFromBelow(0))

        HeroCard(Modifier.enterFromBelow(1), glowColor = BrandRed) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.km_traveled_label), style = MaterialTheme.typography.labelLarge, color = x.onHeroMuted)
                    val km by rememberCountingValue(c.kmTraveled.toLong())
                    Text(stringResource(R.string.km_format, km.toInt()), style = MaterialTheme.typography.displaySmall, color = x.onHero)
                    state.kmHistory.lastOrNull()?.let {
                        Text(stringResource(R.string.km_last_reading, it.date.pretty()), style = MaterialTheme.typography.bodySmall, color = x.onHeroMuted)
                    }
                }
                // La moto sigue rodando: metafora del kilometraje que crece.
                MotoRider(Modifier.width(120.dp).height(100.dp), color = Color.White, running = true)
            }
        }

        if (plan == null) {
            // El ultimo cambio de aceite aun no se guarda en la BD (propuesta del proyecto).
            Card(
                Modifier.fillMaxWidth().enterFromBelow(2).semantics(mergeDescendants = true) {},
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(44.dp).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.OilBarrel, null, tint = MaterialTheme.colorScheme.tertiary) }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.km_oil_coming), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.km_oil_coming_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            val ok = plan.status == MaintenanceStatus.OK
            val container = if (ok) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.errorContainer
            val onContainer = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
            val barColor = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Card(
                Modifier.fillMaxWidth().enterFromBelow(2).semantics(mergeDescendants = true) {},
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(container),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.OilBarrel, null, Modifier.size(18.dp), tint = barColor)
                        Text(stringResource(R.string.km_oil_title), style = MaterialTheme.typography.labelLarge, color = onContainer)
                    }
                    Text(
                        if (plan.status == MaintenanceStatus.OVERDUE) stringResource(R.string.km_over, -plan.kmLeft)
                        else stringResource(R.string.km_left, plan.kmLeft),
                        style = MaterialTheme.typography.headlineSmall,
                        color = onContainer,
                    )
                    // Barra que se llena al entrar.
                    val drawn by rememberDrawnProgress(plan.progress)
                    Box(Modifier.fillMaxWidth().height(10.dp).background(barColor.copy(alpha = 0.18f), MaterialTheme.shapes.small)) {
                        Box(Modifier.fillMaxWidth(drawn.coerceAtLeast(0.01f)).height(10.dp).background(barColor, MaterialTheme.shapes.small))
                    }
                    Text(
                        stringResource(R.string.km_progress, plan.currentKm, plan.nextServiceKm),
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainer,
                    )
                }
            }
        }

        if (state.kmHistory.isNotEmpty()) {
            val sorted = state.kmHistory.sortedBy { it.date }
            val cd = stringResource(R.string.km_chart_cd, sorted.size, sorted.first().km, sorted.last().km)
            Card(
                Modifier.fillMaxWidth().enterFromBelow(3).semantics(mergeDescendants = true) {},
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.km_chart_title), style = MaterialTheme.typography.titleMedium)
                    KmSparkline(sorted.map { it.date to it.km }, cd)
                }
            }
        }
        SectionTitle(stringResource(R.string.km_history), Modifier.enterFromBelow(3))
        val history = state.kmHistory.asReversed()
        if (history.isEmpty()) {
            EmptyState(stringResource(R.string.km_empty), Icons.Rounded.Route, Modifier.enterFromBelow(4), tint = MaterialTheme.colorScheme.primary)
        } else {
            history.forEachIndexed { i, it ->
                TimelineRow(
                    it.date.pretty(), stringResource(R.string.km_format, it.km), last = i == history.lastIndex,
                    modifier = Modifier.enterFromBelow(4 + i.coerceAtMost(6)),
                )
            }
        }
    }
}
