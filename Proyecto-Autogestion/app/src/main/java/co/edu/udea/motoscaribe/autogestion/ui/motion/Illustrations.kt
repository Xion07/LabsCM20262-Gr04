package co.edu.udea.motoscaribe.autogestion.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.ui.theme.BrandRed
import co.edu.udea.motoscaribe.autogestion.ui.theme.BrandRedDeep
import kotlin.math.sin

/**
 * Moto en marcha: ruedas girando, chasis que vibra apenas y carretera discontinua que avanza.
 * Si [running] es falso o las animaciones estan desactivadas, queda quieta.
 */
@Composable
fun MotoRider(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    running: Boolean = true,
    roadColor: Color = color.copy(alpha = 0.45f),
    withRider: Boolean = true,
) {
    val motion = rememberMotionEnabled()
    val phase: State<Float> = if (running && motion) {
        rememberInfiniteTransition(label = "moto").animateFloat(
            0f, 1f, infiniteRepeatable(tween(650, easing = LinearEasing)), label = "wheel",
        )
    } else remember { mutableFloatStateOf(0f) }
    val glow: State<Float> = if (running && motion) {
        rememberInfiniteTransition(label = "lamp").animateFloat(
            0.35f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse), label = "rays",
        )
    } else remember { mutableFloatStateOf(1f) }
    Canvas(modifier.semantics { contentDescription = "" }) {
        // La moto ocupa el cuadrado inscrito (viewport 108) y se centra en el ancho disponible.
        val s = minOf(size.width, size.height) / MotoArt.VIEWPORT
        val t = phase.value
        val bob = if (running && motion) (sin(t * 2 * Math.PI) * 0.9f).toFloat() else 0f
        // Carretera: dashes que avanzan hacia atras (la moto "va hacia adelante")
        val dash = 10f * s
        val roadY = (MotoArt.rearWheel.y + MotoArt.WHEEL_R + 3f) * s
        drawLine(
            roadColor, Offset(0f, roadY), Offset(size.width, roadY), 2.5f * s, StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash), phase = t * dash * 2),
        )
        translate((size.width - MotoArt.VIEWPORT * s) / 2f, 0f) {
            drawMotorcycle(color, s, wheelAngle = t * 360f, bob = bob, rayGlow = glow.value, withRider = withRider)
        }
    }
}

/**
 * Anillo de progreso que se dibuja al entrar. [progress] 0..1. El contenido va centrado.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    strokeWidth: Dp = 11.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    brush: Brush = Brush.sweepGradient(listOf(BrandRedDeep, BrandRed, Color(0xFFFF6B6B), BrandRedDeep)),
    content: @Composable () -> Unit,
) {
    val drawn by rememberDrawnProgress(progress.coerceIn(0f, 1f))
    Box(
        modifier.size(size).drawBehind {
            val sw = strokeWidth.toPx()
            val inset = sw / 2
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            if (drawn > 0f) {
                rotate(-90f) {
                    drawArc(brush, 0f, 360f * drawn, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Check de exito dibujado a mano: primero el circulo, luego el trazo del check. */
@Composable
fun SuccessCheck(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val motion = rememberMotionEnabled()
    val t = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(750, easing = EmphasizedDecelerate)) }
    val measure = remember { PathMeasure() }
    Canvas(modifier.size(88.dp)) {
        val p = t.value
        val sw = 5.dp.toPx()
        val r = size.minDimension / 2 - sw
        val circleT = (p / 0.6f).coerceIn(0f, 1f)
        drawArc(
            color, -90f, 360f * circleT, false,
            Offset(center.x - r, center.y - r), Size(r * 2, r * 2), style = Stroke(sw, cap = StrokeCap.Round),
        )
        val checkT = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f)
        if (checkT > 0f) {
            val path = Path().apply {
                moveTo(center.x - r * 0.45f, center.y + r * 0.02f)
                lineTo(center.x - r * 0.12f, center.y + r * 0.36f)
                lineTo(center.x + r * 0.48f, center.y - r * 0.3f)
            }
            measure.setPath(path, false)
            val seg = Path()
            measure.getSegment(0f, measure.length * checkT, seg, true)
            drawPath(seg, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/**
 * Pantalla de entrada: fondo de marca, la moto cruza y el titulo aparece; luego todo se
 * abre (escala + fundido) y revela la app. Si no hay animaciones, no se muestra.
 */
@Composable
fun IntroOverlay(onFinished: () -> Unit) {
    val motion = rememberMotionEnabled()
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!motion) { onFinished(); return@LaunchedEffect }
        t.animateTo(1f, tween(1500, easing = LinearEasing))
        onFinished()
    }
    if (!motion) return
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                // Ultimo 25 %: se abre y desaparece
                val out = ((t.value - 0.75f) / 0.25f).coerceIn(0f, 1f)
                alpha = 1f - out
                scaleX = 1f + out * 0.12f
                scaleY = scaleX
            }
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(BrandRed, BrandRedDeep)))
                // Halo suave arriba a la derecha
                drawCircle(Color.White.copy(alpha = 0.08f), size.width * 0.55f, Offset(size.width * 0.9f, size.height * 0.1f))
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val entry = (t.value / 0.4f).coerceIn(0f, 1f)
            val eased = EmphasizedDecelerate.transform(entry)
            MotoRider(
                Modifier
                    .width(180.dp)
                    .height(150.dp)
                    .graphicsLayer { alpha = 0.6f + 0.4f * eased },
            )
            Text(
                stringResource(R.string.app_name),
                Modifier.graphicsLayer {
                    val a = ((t.value - 0.3f) / 0.3f).coerceIn(0f, 1f)
                    alpha = a
                    translationY = (1f - EmphasizedDecelerate.transform(a)) * 24.dp.toPx()
                },
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
            )
            Text(
                stringResource(R.string.intro_tagline),
                Modifier.graphicsLayer {
                    alpha = ((t.value - 0.45f) / 0.3f).coerceIn(0f, 1f)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}
