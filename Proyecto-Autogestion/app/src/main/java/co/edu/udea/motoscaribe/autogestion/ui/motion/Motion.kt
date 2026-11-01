package co.edu.udea.motoscaribe.autogestion.ui.motion

import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Curva "expresiva": arranca rapido y asienta suave (Material 3 emphasized decelerate). */
val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

const val DURATION_ENTER = 420
const val DURATION_EXIT = 180
const val DURATION_COUNTER = 850
const val STAGGER_STEP_MS = 55L

/**
 * `true` salvo que el usuario haya puesto "Escala de duracion de animador" en 0 (ajuste de
 * accesibilidad "quitar animaciones"). Las animaciones propias (Canvas, infinitas) se
 * desactivan con esto; las de Compose ya respetan el ajuste por si solas.
 */
@Composable
fun rememberMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}

/**
 * `true` solo la primera vez que una pantalla se compone en esta sesion de la Activity: al rotar
 * o restaurar la pestana el valor guardado hace que el contenido aparezca ya en su sitio.
 */
@Composable
fun rememberEntrance(): Boolean {
    var played by rememberSaveable { mutableStateOf(false) }
    val motion = rememberMotionEnabled()
    val play = remember { motion && !played }
    LaunchedEffect(Unit) { played = true }
    return play
}

/** Entrada escalonada: las tarjetas suben y se funden una tras otra. */
val LocalEntrance = compositionLocalOf { false }

@Composable
fun EntranceScope(play: Boolean = rememberEntrance(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalEntrance provides play, content = content)
}

/**
 * Aparece subiendo y fundiendose con un retraso proporcional a [index]. Lee el progreso en la
 * fase de dibujo (graphicsLayer) para no recomponer en cada frame.
 */
fun Modifier.enterFromBelow(index: Int, distance: Dp = 28.dp): Modifier = composed {
    val play = LocalEntrance.current
    val progress = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(play) {
        if (play && progress.value < 1f) {
            delay(index * STAGGER_STEP_MS)
            progress.animateTo(1f, tween(DURATION_ENTER, easing = EmphasizedDecelerate))
        }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * distance.toPx()
        scaleX = 0.97f + 0.03f * p
        scaleY = scaleX
    }
}

/** Numero que "cuenta" desde el valor anterior (o 0 en la entrada) hasta [target]. */
@Composable
fun rememberCountingValue(target: Long): State<Long> {
    val play = LocalEntrance.current
    val anim = remember { Animatable(if (play) 0f else target.toFloat()) }
    LaunchedEffect(target) {
        anim.animateTo(target.toFloat(), tween(DURATION_COUNTER, easing = EmphasizedDecelerate))
    }
    return remember { androidx.compose.runtime.derivedStateOf { anim.value.toLong() } }
}

/** Progreso 0..1 que se dibuja una vez al entrar y sigue a [target] si cambia. */
@Composable
fun rememberDrawnProgress(target: Float, durationMs: Int = 900): State<Float> {
    val play = LocalEntrance.current
    val anim = remember { Animatable(if (play) 0f else target) }
    LaunchedEffect(target) { anim.animateTo(target, tween(durationMs, easing = EmphasizedDecelerate)) }
    return anim.asState()
}

/** Escala al presionar (0.96) con un "tic" haptico ligero. Para botones y tarjetas tocables. */
fun Modifier.pressFeedback(interaction: MutableInteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val view = LocalView.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            scale.animateTo(0.96f, tween(90, easing = LinearEasing))
        } else {
            scale.animateTo(1f, tween(220, easing = EmphasizedDecelerate))
        }
    }
    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

/** Pulso sutil de opacidad (0.55..1) para avisos urgentes; estatico si no hay animaciones. */
@Composable
fun rememberPulse(active: Boolean): State<Float> {
    val motion = rememberMotionEnabled()
    if (!active || !motion) return remember { mutableStateOf(1f) }
    val transition = rememberInfiniteTransition(label = "pulse")
    return transition.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
}

/** Brillo que recorre un esqueleto de carga; sin animaciones queda un bloque liso. */
fun Modifier.shimmer(shape: Shape = RoundedCornerShape(12.dp)): Modifier = composed {
    val motion = rememberMotionEnabled()
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val hi = MaterialTheme.colorScheme.surfaceContainerLowest
    if (!motion) return@composed background(base, shape)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "shimmerX",
    )
    drawWithCache {
        val w = size.width
        val brush = Brush.linearGradient(
            colors = listOf(base, hi, base),
            start = Offset(x * w, 0f), end = Offset(x * w + w, size.height),
        )
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawBehind { drawOutline(outline, brush) }
    }
}

/** Bloque de esqueleto reutilizable. */
@Composable
fun SkeletonBox(height: Dp, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(12.dp)) {
    Box(modifier.fillMaxWidth().height(height).shimmer(shape))
}
