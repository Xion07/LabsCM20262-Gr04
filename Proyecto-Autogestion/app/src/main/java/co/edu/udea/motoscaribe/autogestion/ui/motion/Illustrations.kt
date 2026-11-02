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

/** Tipo de ilustracion para estados vacios y de error. */
enum class EmptyKind { FINES, PAYMENTS, KM, ERROR }

/**
 * Ilustracion vectorial (Canvas) para estados vacios: escudo con check (sin multas), monedas
 * (sin abonos), carretera con marcador (sin km) y nube desconectada (error). Decorativa: el
 * texto que la acompana lleva el significado.
 */
@Composable
fun EmptyArt(kind: EmptyKind, tint: Color, modifier: Modifier = Modifier, size: Dp = 128.dp) {
    val soft = MaterialTheme.colorScheme.surfaceContainerHighest
    val paper = MaterialTheme.colorScheme.surfaceContainerLowest
    Canvas(modifier.size(size).semantics { contentDescription = "" }) {
        val s = this.size.minDimension
        val c = center
        // Medallon de fondo con dos halos
        drawCircle(tint.copy(alpha = 0.10f), s * 0.5f, c)
        drawCircle(tint.copy(alpha = 0.14f), s * 0.36f, c)
        val stroke = Stroke(s * 0.045f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            EmptyKind.FINES -> {
                val shield = Path().apply {
                    moveTo(c.x, c.y - s * 0.28f)
                    lineTo(c.x + s * 0.2f, c.y - s * 0.2f)
                    lineTo(c.x + s * 0.18f, c.y + s * 0.04f)
                    quadraticBezierTo(c.x + s * 0.14f, c.y + s * 0.2f, c.x, c.y + s * 0.3f)
                    quadraticBezierTo(c.x - s * 0.14f, c.y + s * 0.2f, c.x - s * 0.18f, c.y + s * 0.04f)
                    lineTo(c.x - s * 0.2f, c.y - s * 0.2f)
                    close()
                }
                drawPath(shield, paper)
                drawPath(shield, tint, style = stroke)
                val check = Path().apply {
                    moveTo(c.x - s * 0.1f, c.y + s * 0.01f)
                    lineTo(c.x - s * 0.02f, c.y + s * 0.09f)
                    lineTo(c.x + s * 0.11f, c.y - s * 0.07f)
                }
                drawPath(check, tint, style = stroke)
                // Destellos
                listOf(Offset(c.x + s * 0.3f, c.y - s * 0.3f), Offset(c.x - s * 0.32f, c.y - s * 0.1f), Offset(c.x + s * 0.33f, c.y + s * 0.18f)).forEachIndexed { i, p ->
                    val r = s * (0.035f - i * 0.006f)
                    drawLine(tint, Offset(p.x - r, p.y), Offset(p.x + r, p.y), s * 0.02f, StrokeCap.Round)
                    drawLine(tint, Offset(p.x, p.y - r), Offset(p.x, p.y + r), s * 0.02f, StrokeCap.Round)
                }
            }
            EmptyKind.PAYMENTS -> {
                // Tres monedas apiladas con sombra y una cuarta "entrando"
                val rx = s * 0.2f
                val ry = s * 0.07f
                for (i in 0..2) {
                    val y = c.y + s * 0.16f - i * s * 0.1f
                    drawOval(paper, Offset(c.x - rx, y - ry), Size(rx * 2, ry * 2))
                    drawOval(tint, Offset(c.x - rx, y - ry), Size(rx * 2, ry * 2), style = stroke)
                    drawLine(tint, Offset(c.x - rx, y), Offset(c.x - rx, y + s * 0.1f), stroke.width)
                    drawLine(tint, Offset(c.x + rx, y), Offset(c.x + rx, y + s * 0.1f), stroke.width)
                }
                val coin = Offset(c.x + s * 0.26f, c.y - s * 0.22f)
                drawCircle(paper, s * 0.1f, coin)
                drawCircle(tint, s * 0.1f, coin, style = stroke)
                drawLine(tint, Offset(coin.x - s * 0.045f, coin.y), Offset(coin.x + s * 0.045f, coin.y), s * 0.03f, StrokeCap.Round)
                drawLine(tint, Offset(coin.x, coin.y - s * 0.045f), Offset(coin.x, coin.y + s * 0.045f), s * 0.03f, StrokeCap.Round)
            }
            EmptyKind.KM -> {
                // Carretera en perspectiva con linea discontinua y un marcador al final
                val road = Path().apply {
                    moveTo(c.x - s * 0.3f, c.y + s * 0.3f)
                    lineTo(c.x - s * 0.08f, c.y - s * 0.12f)
                    lineTo(c.x + s * 0.08f, c.y - s * 0.12f)
                    lineTo(c.x + s * 0.3f, c.y + s * 0.3f)
                    close()
                }
                drawPath(road, paper)
                drawPath(road, tint, style = stroke)
                drawLine(
                    tint, Offset(c.x, c.y + s * 0.28f), Offset(c.x, c.y - s * 0.1f), s * 0.03f, StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(s * 0.07f, s * 0.06f)),
                )
                val pin = Offset(c.x, c.y - s * 0.22f)
                val pinPath = Path().apply {
                    moveTo(pin.x, pin.y + s * 0.12f)
                    quadraticBezierTo(pin.x - s * 0.14f, pin.y - s * 0.04f, pin.x - s * 0.1f, pin.y - s * 0.1f)
                    quadraticBezierTo(pin.x, pin.y - s * 0.24f, pin.x + s * 0.1f, pin.y - s * 0.1f)
                    quadraticBezierTo(pin.x + s * 0.14f, pin.y - s * 0.04f, pin.x, pin.y + s * 0.12f)
                    close()
                }
                drawPath(pinPath, tint)
                drawCircle(paper, s * 0.04f, Offset(pin.x, pin.y - s * 0.07f))
            }
            EmptyKind.ERROR -> {
                // Nube con una linea tachada: sin conexion
                val cloud = Path().apply {
                    moveTo(c.x - s * 0.22f, c.y + s * 0.12f)
                    lineTo(c.x + s * 0.2f, c.y + s * 0.12f)
                    arcTo(androidx.compose.ui.geometry.Rect(c.x + s * 0.08f, c.y - s * 0.1f, c.x + s * 0.32f, c.y + s * 0.12f), 90f, -180f, false)
                    arcTo(androidx.compose.ui.geometry.Rect(c.x - s * 0.14f, c.y - s * 0.26f, c.x + s * 0.16f, c.y + s * 0.02f), 20f, -200f, false)
                    arcTo(androidx.compose.ui.geometry.Rect(c.x - s * 0.34f, c.y - s * 0.12f, c.x - s * 0.1f, c.y + s * 0.12f), -60f, -210f, false)
                    close()
                }
                drawPath(cloud, paper)
                drawPath(cloud, tint, style = stroke)
                drawLine(soft, Offset(c.x - s * 0.3f, c.y + s * 0.3f), Offset(c.x + s * 0.3f, c.y - s * 0.3f), s * 0.09f, StrokeCap.Round)
                drawLine(tint, Offset(c.x - s * 0.3f, c.y + s * 0.3f), Offset(c.x + s * 0.3f, c.y - s * 0.3f), s * 0.045f, StrokeCap.Round)
            }
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
