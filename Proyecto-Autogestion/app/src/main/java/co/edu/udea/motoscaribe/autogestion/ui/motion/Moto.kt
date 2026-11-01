package co.edu.udea.motoscaribe.autogestion.ui.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.cos
import kotlin.math.sin

/**
 * Motocicleta urbana de trabajo vista de lado, mirando a la derecha, en un viewport de 108 x 108.
 * Los mismos trazados (pathData) se usan en `res/drawable/ic_launcher_foreground.xml` y
 * `ic_splash_moto.xml`; si se cambia uno hay que cambiar el otro.
 */
object MotoArt {
    const val VIEWPORT = 108f
    val rearWheel = Offset(26f, 74f)
    val frontWheel = Offset(84f, 74f)
    const val WHEEL_R = 12f
    const val TIRE_W = 5f

    /** Carroceria rellena: tanque, asiento y bloque de motor. */
    const val BODY_FILL =
        "M40,52 C44,43 60,41 69,48 L66,59 L42,59 Z " + // tanque
            "M19,52 L42,51 L42,59 L22,61 Q18,61 19,52 Z " + // asiento
            "M44,61 L62,61 Q65,61 65,64 L65,71 Q65,74 62,74 L47,74 Q44,74 44,71 Z" // motor

    /** Guardabarros, horquilla, manubrio, basculante y escape (trazo grueso). */
    const val FRAME_STROKE =
        "M12,70 A14,14 0 0 1 40,70 " + // guardabarros trasero
            "M70,70 A14,14 0 0 1 98,70 " + // guardabarros delantero
            "M84,74 L74,46 " + // horquilla
            "M70,44 L82,39 " + // manubrio
            "M26,74 L46,69 " + // basculante
            "M50,74 L36,81" // escape

    /** Farola (relleno). */
    const val LAMP_FILL = "M74,41 A5,5 0 1 1 73.9,41 Z"

    /** Destello de la farola (trazo fino). */
    const val LAMP_RAYS = "M82,42 L93,37 M83,46 L96,46 M82,50 L93,55"

    /** Piloto con casco (opcional). */
    const val RIDER_STROKE = "M44,53 L47,39 M47,41 L68,44 M44,53 L53,63"
    const val RIDER_HELMET = "M47,24 A7,7 0 1 1 46.9,24 Z"
}

private fun path(d: String): Path = PathParser().parsePathString(d).toPath()

/** Trazados parseados una sola vez. */
private val bodyPath by lazy { path(MotoArt.BODY_FILL) }
private val framePath by lazy { path(MotoArt.FRAME_STROKE) }
private val lampPath by lazy { path(MotoArt.LAMP_FILL) }
private val raysPath by lazy { path(MotoArt.LAMP_RAYS) }
private val riderPath by lazy { path(MotoArt.RIDER_STROKE) }
private val helmetPath by lazy { path(MotoArt.RIDER_HELMET) }

/**
 * Dibuja la moto escalada por [s] (px por unidad de viewport). [wheelAngle] gira los radios;
 * [bob] sube/baja la carroceria (vibracion); [rayGlow] 0..1 modula el destello de la farola.
 */
fun DrawScope.drawMotorcycle(
    color: Color,
    s: Float,
    wheelAngle: Float = 0f,
    bob: Float = 0f,
    rayGlow: Float = 1f,
    withRider: Boolean = true,
) {
    scale(s, s, Offset.Zero) {
        val thick = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Ruedas: llanta gruesa, rin y radios que giran
        listOf(MotoArt.rearWheel, MotoArt.frontWheel).forEach { c ->
            drawCircle(color, MotoArt.WHEEL_R, c, style = Stroke(MotoArt.TIRE_W))
            drawCircle(color, 5.5f, c, style = Stroke(2f))
            rotate(wheelAngle, c) {
                repeat(3) { i ->
                    val a = Math.toRadians(i * 60.0)
                    val dx = (cos(a) * 9f).toFloat(); val dy = (sin(a) * 9f).toFloat()
                    drawLine(color, Offset(c.x - dx, c.y - dy), Offset(c.x + dx, c.y + dy), 1.8f, StrokeCap.Round)
                }
            }
        }
        translate(0f, bob) {
            drawPath(framePath, color, style = thick)
            drawPath(bodyPath, color, style = Fill)
            drawPath(lampPath, color, style = Fill)
            if (rayGlow > 0f) {
                drawPath(raysPath, color.copy(alpha = color.alpha * rayGlow), style = Stroke(2.2f, cap = StrokeCap.Round))
            }
            if (withRider) {
                drawPath(riderPath, color, style = Stroke(4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(helmetPath, color, style = Fill)
            }
        }
    }
}
