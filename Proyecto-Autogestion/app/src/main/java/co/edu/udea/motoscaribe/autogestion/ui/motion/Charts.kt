package co.edu.udea.motoscaribe.autogestion.ui.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Etiqueta corta de fecha para los ejes: "24 oct". */
private fun LocalDate.axisLabel(): String = format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

/**
 * Barras de abonos que crecen al entrar. La ultima barra (abono mas reciente) va en [highlight].
 * Se dibuja con 1 o mas puntos; con 0 el llamador muestra un estado vacio.
 */
@Composable
fun PaymentsBarChart(
    points: List<Pair<LocalDate, Long>>,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    barColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
    highlight: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val drawn by rememberDrawnProgress(1f, 900)
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val max = remember(points) { points.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L }
    Canvas(
        modifier.fillMaxWidth().height(height).semantics { contentDescription = description },
    ) {
        if (points.isEmpty()) return@Canvas
        val labelH = 18.dp.toPx()
        val chartH = size.height - labelH - 6.dp.toPx()
        val n = points.size
        val slot = size.width / n
        val barW = (slot * 0.56f).coerceAtMost(44.dp.toPx())
        val r = CornerRadius(barW / 3f)
        points.forEachIndexed { i, (date, amount) ->
            val fullH = chartH * (amount.toFloat() / max)
            // Cada barra crece con un pequeno retraso respecto a la anterior.
            val local = ((drawn * (1f + n * 0.12f)) - i * 0.12f).coerceIn(0f, 1f)
            val h = (fullH * EmphasizedDecelerate.transform(local)).coerceAtLeast(3.dp.toPx())
            val x = slot * i + (slot - barW) / 2
            val last = i == n - 1
            drawRoundRect(if (last) highlight else barColor, Offset(x, chartH - h), Size(barW, h), r)
            val label = measurer.measure(date.axisLabel(), labelStyle)
            drawText(label, topLeft = Offset(x + barW / 2 - label.size.width / 2, chartH + 6.dp.toPx()))
        }
    }
}

/**
 * Linea del historial de km que se "traza" al entrar, con relleno degradado y punto final.
 * Con un solo punto dibuja solo el punto.
 */
@Composable
fun KmSparkline(
    points: List<Pair<LocalDate, Int>>,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val drawn by rememberDrawnProgress(1f, 1000)
    val measurer = rememberTextMeasurer()
    val labelStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val pathMeasure = remember { PathMeasure() }
    Canvas(
        modifier.fillMaxWidth().height(height).semantics { contentDescription = description },
    ) {
        if (points.isEmpty()) return@Canvas
        val labelH = 18.dp.toPx()
        val padTop = 10.dp.toPx()
        val chartH = size.height - labelH - padTop
        val inset = 8.dp.toPx()
        val w = size.width - inset * 2
        val minKm = points.minOf { it.second }
        val maxKm = points.maxOf { it.second }
        val span = (maxKm - minKm).coerceAtLeast(1)
        val n = points.size
        fun xAt(i: Int) = if (n == 1) inset + w / 2 else inset + w * i / (n - 1)
        fun yAt(km: Int) = padTop + chartH * (1f - (km - minKm).toFloat() / span)
        val sw = 3.dp.toPx()

        if (n > 1) {
            val line = Path().apply {
                moveTo(xAt(0), yAt(points[0].second))
                for (i in 1 until n) lineTo(xAt(i), yAt(points[i].second))
            }
            pathMeasure.setPath(line, false)
            val seg = Path()
            pathMeasure.getSegment(0f, pathMeasure.length * drawn, seg, true)
            // Relleno bajo la parte ya trazada
            if (drawn > 0f) {
                val endPos = pathMeasure.getPosition(pathMeasure.length * drawn)
                val fill = Path().apply {
                    addPath(seg)
                    lineTo(endPos.x, padTop + chartH)
                    lineTo(xAt(0), padTop + chartH)
                    close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f)), padTop, padTop + chartH))
                drawPath(seg, lineColor, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawCircle(lineColor.copy(alpha = 0.25f), 9.dp.toPx(), endPos)
                drawCircle(lineColor, 4.5f.dp.toPx(), endPos)
            }
        } else {
            val c = Offset(xAt(0), yAt(points[0].second))
            drawCircle(lineColor.copy(alpha = 0.25f), 9.dp.toPx(), c)
            drawCircle(lineColor, 4.5f.dp.toPx(), c)
        }
        // Etiquetas: primera y ultima fecha
        val first = measurer.measure(points.first().first.axisLabel(), labelStyle)
        drawText(first, topLeft = Offset(inset, size.height - labelH))
        if (n > 1) {
            val last = measurer.measure(points.last().first.axisLabel(), labelStyle)
            drawText(last, topLeft = Offset(size.width - inset - last.size.width, size.height - labelH))
        }
    }
}
