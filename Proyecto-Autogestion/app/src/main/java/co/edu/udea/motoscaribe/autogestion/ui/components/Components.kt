package co.edu.udea.motoscaribe.autogestion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.data.FineReason
import co.edu.udea.motoscaribe.autogestion.ui.motion.EmptyArt
import co.edu.udea.motoscaribe.autogestion.ui.motion.EmptyKind
import co.edu.udea.motoscaribe.autogestion.ui.motion.EntranceScope
import co.edu.udea.motoscaribe.autogestion.ui.motion.SkeletonBox
import co.edu.udea.motoscaribe.autogestion.ui.motion.enterFromBelow
import co.edu.udea.motoscaribe.autogestion.ui.motion.pressFeedback
import co.edu.udea.motoscaribe.autogestion.ui.theme.MotosTheme

/** Ancho maximo del contenido: en tablet u horizontal no se estira a todo el ancho. */
val ContentMaxWidth = 640.dp

/** Ancho maximo cuando la pantalla es expandida (>= 840 dp) y el contenido va a dos columnas. */
val ContentMaxWidthExpanded = 1080.dp

/** Separacion vertical estandar entre bloques de una pantalla. */
val BlockGap = 14.dp

/** Ancho (dp) desde el que las pantallas se organizan en dos columnas (tablet u horizontal). */
const val EXPANDED_DP = 840

/** `true` en tablet u horizontal ancho: las pantallas reparten su contenido en dos columnas. */
@Composable
fun isExpandedWidth(): Boolean = LocalConfiguration.current.screenWidthDp >= EXPANDED_DP

/** Pantalla con scroll vertical, contenido centrado, limitado en ancho y con entrada escalonada. */
@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val maxWidth = if (isExpandedWidth()) ContentMaxWidthExpanded else ContentMaxWidth
    EntranceScope {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(BlockGap),
                content = content,
            )
        }
    }
}

/**
 * Dos columnas en pantallas expandidas; en telefono [primary] y luego [secondary] en vertical.
 * Cada columna conserva la separacion estandar entre bloques.
 */
@Composable
fun ColumnScope.Adaptive(
    primary: @Composable ColumnScope.() -> Unit,
    secondary: @Composable ColumnScope.() -> Unit,
    primaryWeight: Float = 1f,
) {
    if (isExpandedWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(primaryWeight), verticalArrangement = Arrangement.spacedBy(BlockGap), content = primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BlockGap), content = secondary)
        }
    } else {
        primary()
        secondary()
    }
}

/** Esqueleto con brillo mientras llegan los datos (en vez de un spinner). */
@Composable
fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SkeletonBox(28.dp, Modifier.fillMaxWidth(0.55f))
            SkeletonBox(168.dp, shape = MaterialTheme.shapes.large)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBox(96.dp, Modifier.weight(1f), MaterialTheme.shapes.medium)
                SkeletonBox(96.dp, Modifier.weight(1f), MaterialTheme.shapes.medium)
            }
            SkeletonBox(20.dp, Modifier.fillMaxWidth(0.4f))
            SkeletonBox(72.dp, shape = MaterialTheme.shapes.medium)
            SkeletonBox(72.dp, shape = MaterialTheme.shapes.medium)
        }
    }
}

/** Cabecera de pantalla: titulo grande y subtitulo opcional. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(top = 6.dp).semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/** Estado vacio con ilustracion dibujada en Canvas, titulo y mensaje amable. */
@Composable
fun EmptyState(
    title: String,
    body: String?,
    kind: EmptyKind,
    modifier: Modifier = Modifier,
    tint: Color = MotosTheme.colors.success,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 12.dp).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        EmptyArt(kind, tint, Modifier.padding(bottom = 6.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (body != null) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Fila de [StatCard]s de la misma altura. */
@Composable
fun StatRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Chip informativo con icono: para cifras cortas ("3 multas", "Cuota 12"). */
@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, container: Color = MaterialTheme.colorScheme.surfaceContainerHigh) {
    Row(
        modifier
            .background(container, CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(14.dp), tint = color)
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Dato con icono, titulo pequeno y valor destacado (lee junto en TalkBack). */
@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    // fillMaxHeight: en una fila con height(IntrinsicSize.Min) todas las tarjetas quedan igual de altas.
    Card(
        modifier.fillMaxHeight().semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(34.dp).background(accent.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, Modifier.size(18.dp), tint = accent) }
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Chip de estado con punto de color; [glow] 0..1 modula el halo del punto (pulso). */
@Composable
fun StatusChip(text: String, color: Color, container: Color, onContainer: Color, modifier: Modifier = Modifier, glow: Float = 1f) {
    Row(
        modifier
            .background(container, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(10.dp).drawBehind {
                drawCircle(color.copy(alpha = 0.35f * glow), radius = size.minDimension * (0.6f + 0.5f * glow))
                drawCircle(color)
            },
        )
        Text(text, style = MaterialTheme.typography.labelMedium, color = onContainer)
    }
}

/** Tarjeta protagonista: superficie oscura con degradado y un halo del color del estado. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, glowColor: Color = Color.Transparent, content: @Composable ColumnScope.() -> Unit) {
    val start = MotosTheme.colors.heroStart
    val end = MotosTheme.colors.heroEnd
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(start, end), Offset.Zero, Offset(size.width, size.height)))
                drawCircle(
                    Brush.radialGradient(listOf(glowColor.copy(alpha = 0.3f), Color.Transparent), Offset(size.width * 0.92f, size.height * 0.08f), size.width * 0.5f),
                    radius = size.width * 0.5f, center = Offset(size.width * 0.92f, size.height * 0.08f),
                )
                drawCircle(Color.White.copy(alpha = 0.04f), size.width * 0.45f, Offset(size.width * 0.05f, size.height * 1.05f))
            }
            .padding(22.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
fun AlertCard(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, action: @Composable (() -> Unit)? = null) {
    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (icon != null) Icon(icon, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Column(Modifier.weight(1f)) {
                Text(text, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                action?.invoke()
            }
        }
    }
}

/** Fila de monto con icono en medallon: titulo y subtitulo a la izquierda, importe a la derecha. */
@Composable
fun AmountRow(title: String, subtitle: String, amount: String, icon: ImageVector, modifier: Modifier = Modifier, amountColor: Color = MaterialTheme.colorScheme.error) {
    Card(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).background(amountColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, Modifier.size(20.dp), tint = amountColor) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(amount, color = amountColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** Fila de historial (fecha a la izquierda, valor a la derecha) con punto de linea de tiempo. */
@Composable
fun TimelineRow(primary: String, secondary: String, last: Boolean, modifier: Modifier = Modifier, dotColor: Color = MaterialTheme.colorScheme.primary) {
    Row(modifier.fillMaxWidth().heightIn(min = 52.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(24.dp).fillMaxSize().drawBehind {
                val cx = size.width / 2
                drawCircle(dotColor.copy(alpha = 0.25f), 8.dp.toPx(), Offset(cx, size.height / 2))
                drawCircle(dotColor, 4.dp.toPx(), Offset(cx, size.height / 2))
            },
        )
        Spacer(Modifier.width(12.dp))
        Text(primary, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(secondary, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun FineReason.label(): String = stringResource(
    when (this) {
        FineReason.LATE_PAYMENT -> R.string.fine_reason_late
        FineReason.OTHER -> R.string.fine_reason_other
    }
)

/** Boton principal lleno, alto 52 dp, con escala y haptico al presionar. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, loading: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp).pressFeedback(interaction),
        interactionSource = interaction,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(),
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun TonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val interaction = remember { MutableInteractionSource() }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp).pressFeedback(interaction),
        interactionSource = interaction,
        shape = MaterialTheme.shapes.medium,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val interaction = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp).pressFeedback(interaction),
        interactionSource = interaction,
        shape = MaterialTheme.shapes.medium,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Fallo al cargar los datos: ilustracion, mensaje y boton para reintentar. */
@Composable
fun ErrorState(title: String, body: String, retryLabel: String, onRetry: () -> Unit) {
    EntranceScope {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 360.dp).padding(24.dp).enterFromBelow(0),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EmptyState(title, body, EmptyKind.ERROR, tint = MaterialTheme.colorScheme.error)
                PrimaryButton(retryLabel, onRetry, icon = Icons.Rounded.Refresh)
            }
        }
    }
}

/** Fila de monto con un chip de contexto (p. ej. numero de cuota) bajo el titulo. */
@Composable
fun AmountRowChip(title: String, chip: String, subtitle: String, amount: String, icon: ImageVector, modifier: Modifier = Modifier, amountColor: Color = MaterialTheme.colorScheme.error) {
    Card(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).background(amountColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, Modifier.size(20.dp), tint = amountColor) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(chip)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(amount, color = amountColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** Separador vertical de 2 dp para dar aire entre bloques. */
@Composable
fun Gap(height: androidx.compose.ui.unit.Dp = 2.dp) = Spacer(Modifier.height(height))
