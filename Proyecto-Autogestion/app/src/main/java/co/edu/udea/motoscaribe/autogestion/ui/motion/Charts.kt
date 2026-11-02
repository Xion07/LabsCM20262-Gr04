package co.edu.udea.motoscaribe.autogestion.ui.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Etiqueta corta de fecha para los ejes: "24 oct". */
private fun LocalDate.axisLabel(): String = format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

/** Lineas guia horizontales discretas (tres niveles) para que el grafico no flote en blanco. */
private fun DrawScope.drawGuides(top: Float, bottom: Float, color: Color) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))
    for (k in 0..2) {
        val y = top + (bottom - top) * k / 2f
        drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = dash)
    }
}

/** Globo de texto con flecha, centrado en [anchorX] y apoyado sobre [anchorY]; no se sale del ancho. */
private fun DrawScope.drawTooltip(
    anchorX: Float, anchorY: Float,
    title: TextLayoutResult, body: TextLayoutResult,
    background: Color, onBackground: Color,
) {
    val padX = 10.dp.toPx()
    val padY = 6.dp.toPx()
    val gap = 2.dp.toPx()
    val tipH = 5.dp.toPx()
    val w = maxOf(title.size.width, body.size.width) + padX * 2
    val h = title.size.height + body.size.height + gap + padY * 2
    val left = (anchorX - w / 2).coerceIn(0f, (size.width - w).coerceAtLeast(0f))
    val top = (anchorY - tipH - h).coerceAtLeast(0f)
    drawRoundRect(background, Offset(left, top), Size(w, h), CornerRadius(8.dp.toPx()))
    val tip = Path().apply {
        moveTo(anchorX - tipH, top + h)
        lineTo(anchorX, top + h + tipH)
        lineTo(anchorX + tipH, top + h)
        close()
    }
    drawPath(tip, background)
    drawText(title, color = onBackground, topLeft = Offset(left + padX, top + padY))
    drawText(body, color = onBackground, topLeft = Offset(left + padX, top + padY + title.size.height + gap))
}

/**
 * Barras de abonos que crecen al entrar. La ultima barra (abono mas reciente) va en [highlight].
 * Tocar o arrastrar sobre una barra la selecciona y muestra fecha y monto en un globo (con un
 * "tic" haptico al cambiar). Se dibuja con 1 o mas puntos; con 0 el llamador muestra un estado vacio.
 */
@Composable
fun PaymentsBarChart(
    points: List<Pair<LocalDate, Long>>,
    description: String,
    formatAmount: (Long) -> String,
    formatDate: (LocalDate) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 184.dp,
    barColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
    highlight: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    guideColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    tooltipColor: Color = MaterialTheme.colorScheme.inverseSurface,
    onTooltipColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
) {
    val drawn by rememberDrawnProgress(1f, 900)
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val tipTitle = MaterialTheme.typography.labelSmall
    val tipBody = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    val max = remember(points) { points.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L }
    var selected by rememberSaveable(points) { mutableIntStateOf(-1) }
    val tick = rememberHapticTick()
    val n = points.size
    fun select(x: Float, width: Float) {
        if (n == 0 || width <= 0f) return
        val i = (x / (width / n)).toInt().coerceIn(0, n - 1)
        if (i != selected) { selected = i; tick() }
    }
    val selectedText = points.getOrNull(selected)?.let { "${formatDate(it.first)}: ${formatAmount(it.second)}" }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                contentDescription = description
                if (selectedText != null) stateDescription = selectedText
            }
            .pointerInput(points) { detectTapGestures { select(it.x, size.width.toFloat()) } }
            .pointerInput(points) {
                detectHorizontalDragGestures { change, _ -> select(change.position.x, size.width.toFloat()); change.consume() }
            },
    ) {
        if (points.isEmpty()) return@Canvas
        val tooltipH = 44.dp.toPx()
        val labelH = 18.dp.toPx()
        val chartTop = tooltipH
        val chartBottom = size.height - labelH - 6.dp.toPx()
        val chartH = chartBottom - chartTop
        val slot = size.width / n
        val barW = (slot * 0.56f).coerceAtMost(44.dp.toPx())
        val r = CornerRadius(barW / 3f)
        drawGuides(chartTop, chartBottom, guideColor)
        points.forEachIndexed { i, (date, amount) ->
            val fullH = chartH * (amount.toFloat() / max)
            // Cada barra crece con un pequeno retraso respecto a la anterior.
            val local = ((drawn * (1f + n * 0.12f)) - i * 0.12f).coerceIn(0f, 1f)
            val h = (fullH * EmphasizedDecelerate.transform(local)).coerceAtLeast(3.dp.toPx())
            val x = slot * i + (slot - barW) / 2
            val last = i == n - 1
            val isSel = i == selected
            val color = when {
                isSel -> highlight
                last -> highlight.copy(alpha = if (selected >= 0) 0.55f else 1f)
                else -> barColor
            }
            if (isSel) {
                // Columna resaltada detras de la barra elegida.
                drawRoundRect(highlight.copy(alpha = 0.10f), Offset(slot * i + 2.dp.toPx(), chartTop), Size(slot - 4.dp.toPx(), chartH), CornerRadius(10.dp.toPx()))
            }
            drawRoundRect(color, Offset(x, chartBottom - h), Size(barW, h), r)
            val label = measurer.measure(date.axisLabel(), labelStyle)
            drawText(label, topLeft = Offset(x + barW / 2 - label.size.width / 2, chartBottom + 6.dp.toPx()))
            if (isSel) {
                val t = measurer.measure(formatDate(date), tipTitle)
                val b = measurer.measure(formatAmount(amount), tipBody)
                drawTooltip(x + barW / 2, chartBottom - h - 4.dp.toPx(), t, b, tooltipColor, onTooltipColor)
            }
        }
    }
}

/**
 * Linea del historial de km que se "traza" al entrar, con relleno degradado y punto final.
 * Con un solo punto lo centra, lo marca con una guia y escribe su valor encima.
 */
@Composable
fun KmSparkline(
    points: List<Pair<LocalDate, Int>>,
    description: String,
    formatKm: (Int) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 112.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    guideColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
) {
    val drawn by rememberDrawnProgress(1f, 1000)
    val measurer = rememberTextMeasurer()
    val labelStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val valueStyle: TextStyle = MaterialTheme.typography.labelMedium.copy(color = lineColor, fontWeight = FontWeight.Bold)
    val pathMeasure = remember { PathMeasure() }
    Canvas(
        modifier.fillMaxWidth().height(height).semantics { contentDescription = description },
    ) {
        if (points.isEmpty()) return@Canvas
        val labelH = 18.dp.toPx()
        val padTop = 22.dp.toPx() // sitio para el valor sobre el ultimo punto
        val chartH = size.height - labelH - padTop
        val inset = 10.dp.toPx()
        val w = size.width - inset * 2
        val minKm = points.minOf { it.second }
        val maxKm = points.maxOf { it.second }
        val span = (maxKm - minKm).coerceAtLeast(1)
        val n = points.size
        fun xAt(i: Int) = if (n == 1) inset + w / 2 else inset + w * i / (n - 1)
        fun yAt(km: Int) = if (n == 1) padTop + chartH / 2 else padTop + chartH * (1f - (km - minKm).toFloat() / span)
        val sw = 3.dp.toPx()
        drawGuides(padTop, padTop + chartH, guideColor)

        fun drawEndDot(c: Offset, km: Int) {
            drawCircle(lineColor.copy(alpha = 0.25f), 9.dp.toPx(), c)
            drawCircle(lineColor, 4.5f.dp.toPx(), c)
            val v = measurer.measure(formatKm(km), valueStyle)
            val left = (c.x - v.size.width / 2).coerceIn(0f, (size.width - v.size.width).coerceAtLeast(0f))
            drawText(v, topLeft = Offset(left, (c.y - 12.dp.toPx() - v.size.height).coerceAtLeast(0f)))
        }

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
                // Puntos intermedios pequenos
                for (i in 0 until n - 1) {
                    if (xAt(i) <= endPos.x) drawCircle(lineColor, 2.5f.dp.toPx(), Offset(xAt(i), yAt(points[i].second)))
                }
                if (drawn >= 1f) drawEndDot(endPos, points.last().second) else {
                    drawCircle(lineColor.copy(alpha = 0.25f), 9.dp.toPx(), endPos)
                    drawCircle(lineColor, 4.5f.dp.toPx(), endPos)
                }
            }
            val first = measurer.measure(points.first().first.axisLabel(), labelStyle)
            drawText(first, topLeft = Offset(inset, size.height - labelH))
            val last = measurer.measure(points.last().first.axisLabel(), labelStyle)
            drawText(last, topLeft = Offset(size.width - inset - last.size.width, size.height - labelH))
        } else {
            // Un solo punto: guia horizontal, punto centrado y fecha debajo, centrada.
            val c = Offset(xAt(0), yAt(points[0].second))
            drawLine(
                lineColor.copy(alpha = 0.35f), Offset(inset, c.y), Offset(size.width - inset, c.y), 1.5f.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
            )
            drawEndDot(c, points[0].second)
            val only = measurer.measure(points.first().first.axisLabel(), labelStyle)
            drawText(only, topLeft = Offset(c.x - only.size.width / 2, size.height - labelH))
        }
    }
}
