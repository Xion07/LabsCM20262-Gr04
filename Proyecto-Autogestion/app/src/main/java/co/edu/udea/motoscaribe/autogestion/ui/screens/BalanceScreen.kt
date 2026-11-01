package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.ui.components.ContentMaxWidth
import co.edu.udea.motoscaribe.autogestion.ui.components.EmptyState
import co.edu.udea.motoscaribe.autogestion.ui.components.HeroCard
import co.edu.udea.motoscaribe.autogestion.ui.components.Loading
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenHeader
import co.edu.udea.motoscaribe.autogestion.ui.components.SectionTitle
import co.edu.udea.motoscaribe.autogestion.ui.components.TimelineRow
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.motion.EntranceScope
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
    val x = MotosTheme.colors
    val paid = (c.totalValue - c.balance).coerceAtLeast(0)
    val fraction = paidFraction(c.balance, c.totalValue)
    EntranceScope {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    ScreenHeader(stringResource(R.string.nav_balance), stringResource(R.string.balance_subtitle), Modifier.enterFromBelow(0))
                }
                item {
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
                item {
                    Row(Modifier.enterFromBelow(2), verticalAlignment = Alignment.CenterVertically) {
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
                    item { EmptyState(stringResource(R.string.balance_empty), Icons.Rounded.Savings, Modifier.enterFromBelow(3), tint = MaterialTheme.colorScheme.primary) }
                } else {
                    itemsIndexed(state.payments) { i, p ->
                        TimelineRow(
                            p.date.pretty(), p.amount.cop(), last = i == state.payments.lastIndex,
                            modifier = Modifier.enterFromBelow(3 + i.coerceAtMost(6)),
                            dotColor = x.success,
                        )
                    }
                }
            }
        }
    }
}
