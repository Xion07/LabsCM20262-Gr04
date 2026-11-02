package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
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
import co.edu.udea.motoscaribe.autogestion.data.Contract
import co.edu.udea.motoscaribe.autogestion.data.Payment
import co.edu.udea.motoscaribe.autogestion.ui.components.BlockGap
import co.edu.udea.motoscaribe.autogestion.ui.components.ContentMaxWidth
import co.edu.udea.motoscaribe.autogestion.ui.components.ContentMaxWidthExpanded
import co.edu.udea.motoscaribe.autogestion.ui.components.EmptyState
import co.edu.udea.motoscaribe.autogestion.ui.components.HeroCard
import co.edu.udea.motoscaribe.autogestion.ui.components.Loading
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenHeader
import co.edu.udea.motoscaribe.autogestion.ui.components.SectionTitle
import co.edu.udea.motoscaribe.autogestion.ui.components.TimelineRow
import co.edu.udea.motoscaribe.autogestion.ui.components.isExpandedWidth
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.motion.EmptyKind
import co.edu.udea.motoscaribe.autogestion.ui.motion.EntranceScope
import co.edu.udea.motoscaribe.autogestion.ui.motion.PaymentsBarChart
import co.edu.udea.motoscaribe.autogestion.ui.motion.enterFromBelow
import co.edu.udea.motoscaribe.autogestion.ui.motion.rememberCountingValue
import co.edu.udea.motoscaribe.autogestion.ui.motion.rememberDrawnProgress
import co.edu.udea.motoscaribe.autogestion.ui.pretty
import co.edu.udea.motoscaribe.autogestion.ui.theme.BrandRed
import co.edu.udea.motoscaribe.autogestion.ui.theme.MotosTheme

@Composable
fun BalanceScreen(state: UiState) {
    val c = state.contract
    if (c == null) {
        Loading()
        return
    }
    val expanded = isExpandedWidth()
    EntranceScope {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = if (expanded) ContentMaxWidthExpanded else ContentMaxWidth).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(BlockGap),
            ) {
                item {
                    ScreenHeader(stringResource(R.string.nav_balance), stringResource(R.string.balance_subtitle), Modifier.enterFromBelow(0))
                }
                if (expanded) {
                    // Tablet u horizontal: la moto a la izquierda y el grafico de abonos a la derecha.
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column(Modifier.weight(1f)) { BalanceHero(c) }
                            Column(Modifier.weight(1f)) { PaymentsCard(state.payments) }
                        }
                    }
                } else {
                    item { BalanceHero(c) }
                    if (state.payments.isNotEmpty()) item { PaymentsCard(state.payments) }
                }
                item {
                    Row(Modifier.enterFromBelow(3), verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(stringResource(R.string.balance_history), Modifier.weight(1f))
                        if (state.payments.isNotEmpty()) {
                            Text(
                                pluralStringResource(R.plurals.payments_count, state.payments.size, state.payments.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (state.payments.isEmpty()) {
                    item {
                        EmptyState(
                            stringResource(R.string.balance_empty), stringResource(R.string.balance_empty_body),
                            EmptyKind.PAYMENTS, Modifier.enterFromBelow(4), tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    itemsIndexed(state.payments) { i, p ->
                        TimelineRow(
                            p.date.pretty(), p.amount.cop(), last = i == state.payments.lastIndex,
                            modifier = Modifier.enterFromBelow(4 + i.coerceAtMost(6)),
                            dotColor = MotosTheme.colors.success,
                        )
                    }
                }
            }
        }
    }
}

/** Tarjeta oscura con el valor de la moto y la barra abonado / pendiente. */
@Composable
private fun BalanceHero(c: Contract) {
    val x = MotosTheme.colors
    val paid = (c.totalValue - c.balance).coerceAtLeast(0)
    val fraction = paidFraction(c.balance, c.totalValue)
    HeroCard(Modifier.enterFromBelow(1), glowColor = BrandRed) {
        Text(stringResource(R.string.balance_total), style = MaterialTheme.typography.labelLarge, color = x.onHeroMuted)
        val total by rememberCountingValue(c.totalValue)
        Text(total.cop(), style = MaterialTheme.typography.displaySmall, color = x.onHero)
        Spacer(Modifier.height(8.dp))
        // Barra abonado / pendiente que se llena al entrar.
        val drawn by rememberDrawnProgress(fraction)
        Box(
            Modifier.fillMaxWidth().height(10.dp)
                .background(Color.White.copy(alpha = 0.16f), MaterialTheme.shapes.small),
        ) {
            Box(
                Modifier.fillMaxWidth(drawn.coerceAtLeast(0.01f)).height(10.dp)
                    .background(x.heroSuccess, MaterialTheme.shapes.small),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(R.string.balance_paid), style = MaterialTheme.typography.labelMedium, color = x.heroSuccess)
                Text(paid.cop(), style = MaterialTheme.typography.titleMedium, color = x.onHero)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.balance_pending), style = MaterialTheme.typography.labelMedium, color = x.onHeroMuted)
                Text(c.balance.cop(), style = MaterialTheme.typography.titleMedium, color = x.onHero)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.Speed, null, Modifier.size(16.dp), tint = x.onHeroMuted)
            Text(stringResource(R.string.balance_km, c.kmTraveled), style = MaterialTheme.typography.bodySmall, color = x.onHeroMuted)
        }
    }
}

/** Tarjeta con el grafico de abonos (tocable) o, si no hay, el estado vacio ilustrado. */
@Composable
private fun PaymentsCard(payments: List<Payment>) {
    val x = MotosTheme.colors
    if (payments.isEmpty()) {
        EmptyState(
            stringResource(R.string.balance_empty), stringResource(R.string.balance_empty_body),
            EmptyKind.PAYMENTS, Modifier.enterFromBelow(2), tint = MaterialTheme.colorScheme.primary,
        )
        return
    }
    // La API entrega los abonos del mas reciente al mas antiguo; el grafico va en orden cronologico.
    val shown = payments.sortedBy { it.date }.takeLast(8)
    val last = shown.last()
    val cd = stringResource(R.string.balance_chart_cd, payments.size, last.date.pretty(), last.amount.cop())
    Card(
        Modifier.fillMaxWidth().enterFromBelow(2).semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.balance_chart_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.balance_chart_last, last.date.pretty()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(last.amount.cop(), style = MaterialTheme.typography.titleLarge, color = x.success)
            }
            PaymentsBarChart(
                shown.map { it.date to it.amount }, cd,
                formatAmount = { it.cop() }, formatDate = { it.pretty() },
                highlight = x.success, barColor = x.success.copy(alpha = 0.3f),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.TouchApp, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.balance_chart_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
