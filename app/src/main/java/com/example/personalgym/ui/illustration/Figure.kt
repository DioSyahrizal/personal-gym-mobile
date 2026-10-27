package com.example.personalgym.ui.illustration

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** A point in the 100×100 illustration space (y grows downward, floor at y = 92). */
data class Pt(val x: Float, val y: Float) {
    operator fun plus(o: Pt) = Pt(x + o.x, y + o.y)
    fun lerp(o: Pt, t: Float) = Pt(x + (o.x - x) * t, y + (o.y - y) * t)
    fun mirror() = Pt(100f - x, y)
}

fun P(x: Number, y: Number) = Pt(x.toFloat(), y.toFloat())

/**
 * Joint positions of the figure. "N" is the limb nearest to the viewer, "F" the far one.
 * For [front]-facing poses the near limbs are on the right side of the picture.
 */
data class Pose(
    val head: Pt, val sh: Pt, val hip: Pt,
    val elbowN: Pt, val handN: Pt, val elbowF: Pt, val handF: Pt,
    val kneeN: Pt, val footN: Pt, val kneeF: Pt, val footF: Pt,
    val toeN: Pt, val toeF: Pt,
    val front: Boolean,
) {
    fun lerp(o: Pose, t: Float) = Pose(
        head.lerp(o.head, t), sh.lerp(o.sh, t), hip.lerp(o.hip, t),
        elbowN.lerp(o.elbowN, t), handN.lerp(o.handN, t), elbowF.lerp(o.elbowF, t), handF.lerp(o.handF, t),
        kneeN.lerp(o.kneeN, t), footN.lerp(o.footN, t), kneeF.lerp(o.kneeF, t), footF.lerp(o.footF, t),
        toeN.lerp(o.toeN, t), toeF.lerp(o.toeF, t), front,
    )

    /** Swap sides — handy for alternating moves. */
    fun mirrored() = Pose(
        head.mirror(), sh.mirror(), hip.mirror(),
        elbowF.mirror(), handF.mirror(), elbowN.mirror(), handN.mirror(),
        kneeF.mirror(), footF.mirror(), kneeN.mirror(), footN.mirror(),
        toeF.mirror(), toeN.mirror(), front,
    )

    fun shift(dx: Number, dy: Number): Pose {
        val d = P(dx, dy)
        return Pose(
            head + d, sh + d, hip + d, elbowN + d, handN + d, elbowF + d, handF + d,
            kneeN + d, footN + d, kneeF + d, footF + d, toeN + d, toeF + d, front,
        )
    }
}

private val FAR = P(-2, -0.6)

/** Side-view pose facing right. Far limbs default to the near ones, nudged back for depth. */
fun side(
    head: Pt, sh: Pt, hip: Pt,
    elbow: Pt, hand: Pt, knee: Pt, foot: Pt,
    elbowF: Pt? = null, handF: Pt? = null, kneeF: Pt? = null, footF: Pt? = null,
    toe: Pt? = null, toeF: Pt? = null,
): Pose {
    val fFoot = footF ?: (foot + FAR)
    return Pose(
        head, sh, hip,
        elbow, hand, elbowF ?: (elbow + FAR), handF ?: (hand + FAR),
        knee, foot, kneeF ?: (knee + FAR), fFoot,
        toe ?: (foot + P(5, 0.5)), toeF ?: (fFoot + P(5, 0.5)),
        front = false,
    )
}

/** Front-view pose; the far (left) side mirrors the near (right) side unless given. */
fun front(
    head: Pt, sh: Pt, hip: Pt,
    elbow: Pt, hand: Pt, knee: Pt, foot: Pt,
    elbowF: Pt? = null, handF: Pt? = null, kneeF: Pt? = null, footF: Pt? = null,
): Pose {
    val fFoot = footF ?: foot.mirror()
    return Pose(
        head, sh, hip,
        elbow, hand, elbowF ?: elbow.mirror(), handF ?: hand.mirror(),
        knee, foot, kneeF ?: knee.mirror(), fFoot,
        foot + P(2, 1), fFoot + P(-2, 1),
        front = true,
    )
}

enum class HandProp { NONE, DUMBBELL, BARBELL, KETTLEBELL, HANDLE, WIDE_BAR }

enum class Scene {
    NONE, MAT, BENCH, BENCH_TOP, INCLINE_BENCH, SEAT, SEAT_FRONT, PULLUP_BAR,
    CABLE_HIGH, CABLE_MID, CABLE_DUAL, CABLE_PULLDOWN, CABLE_ROW, LEG_PRESS,
    TREADMILL, ROWER, BOX, STEP, LOW_BENCH,
}

data class Illustration(
    val a: Pose,
    val b: Pose,
    val hand: HandProp = HandProp.NONE,
    val scene: Scene = Scene.NONE,
    /** Draw a machine roller pad at the near foot (leg extension / leg curl). */
    val footRoller: Boolean = false,
    /** Draw a sled plate at the feet (leg press). */
    val footPlate: Boolean = false,
    /** Weight rests on the hips (hip thrust) rather than in the hands. */
    val weightAtHip: Boolean = false,
    val periodMs: Int = 1500,
)

data class FigureColors(
    val body: Color,
    val bodyFar: Color,
    val equipment: Color,
    val accent: Color,
    val floor: Color,
    /** Thin edge drawn around near limbs so they separate from the torso. */
    val outline: Color,
)

@Composable
fun ExerciseFigure(
    illustration: Illustration,
    colors: FigureColors,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val t = if (animate) {
        val transition = rememberInfiniteTransition(label = "figure")
        val v by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(illustration.periodMs, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "t",
        )
        v
    } else 0.5f

    Canvas(modifier) {
        val s = min(size.width, size.height) / 100f
        val ox = (size.width - 100f * s) / 2f
        val oy = (size.height - 100f * s) / 2f
        val pose = illustration.a.lerp(illustration.b, t)
        FigurePainter(this, s, ox, oy, colors).draw(illustration, pose)
    }
}

private class FigurePainter(
    val scope: DrawScope,
    val s: Float,
    val ox: Float,
    val oy: Float,
    val c: FigureColors,
) {
    fun o(p: Pt) = Offset(ox + p.x * s, oy + p.y * s)
    fun o(x: Number, y: Number) = o(P(x, y))

    fun line(a: Pt, b: Pt, color: Color, w: Float) =
        scope.drawLine(color, o(a), o(b), strokeWidth = w * s, cap = StrokeCap.Round)

    fun rect(x: Number, y: Number, w: Number, h: Number, color: Color, r: Float = 1.5f) =
        scope.drawRoundRect(
            color, o(x, y), Size(w.toFloat() * s, h.toFloat() * s), CornerRadius(r * s),
        )

    fun circle(center: Pt, r: Float, color: Color) = scope.drawCircle(color, r * s, o(center))

    fun draw(ill: Illustration, p: Pose) {
        // Floor
        line(P(4, 92.5), P(96, 92.5), c.floor, 1.2f)
        drawScene(ill.scene, p)

        val limb = 4.2f
        val torso = 5.5f
        val farColor = if (p.front) c.body else c.bodyFar
        val shN = if (p.front) p.sh + P(8, 1) else p.sh
        val shF = if (p.front) p.sh + P(-8, 1) else p.sh
        val hipN = if (p.front) p.hip + P(4.5, 0) else p.hip
        val hipF = if (p.front) p.hip + P(-4.5, 0) else p.hip

        // Far limbs (behind)
        line(hipF, p.kneeF, farColor, limb)
        line(p.kneeF, p.footF, farColor, limb)
        line(p.footF, p.toeF, farColor, limb * 0.8f)
        line(shF, p.elbowF, farColor, limb * 0.9f)
        line(p.elbowF, p.handF, farColor, limb * 0.85f)
        if (!p.front) handProp(ill, p.handF, p.elbowF, far = true)

        // Torso + head
        if (p.front) {
            val path = Path().apply {
                moveTo(o(shF).x, o(shF).y)
                lineTo(o(shN).x, o(shN).y)
                lineTo(o(hipN).x, o(hipN).y)
                lineTo(o(hipF).x, o(hipF).y)
                close()
            }
            scope.drawPath(path, c.body)
            scope.drawPath(path, c.body, style = Stroke(width = 3f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            line(p.sh, p.head, c.body, 3f)
        } else {
            line(p.sh, p.hip, c.body, torso)
            line(p.sh, p.sh.lerp(p.head, 0.45f), c.body, 3f)
        }
        circle(p.head, 5.2f, c.body)

        // Near limbs
        val edge = 1.6f
        line(hipN, p.kneeN, c.outline, limb + edge)
        line(p.kneeN, p.footN, c.outline, limb + edge)
        line(hipN, p.kneeN, c.body, limb)
        line(p.kneeN, p.footN, c.body, limb)
        line(p.footN, p.toeN, c.body, limb * 0.8f)
        if (ill.footRoller) circle(p.footN + P(1.5, -1), 3.2f, c.accent)
        if (ill.footPlate) footPlate(p)
        line(shN, p.elbowN, c.outline, limb * 0.9f + edge)
        line(p.elbowN, p.handN, c.outline, limb * 0.85f + edge)
        line(shN, p.elbowN, c.body, limb * 0.9f)
        line(p.elbowN, p.handN, c.body, limb * 0.85f)
        if (p.front) handProp(ill, p.handF, p.elbowF, far = false)
        handProp(ill, p.handN, p.elbowN, far = false)
        if (ill.hand == HandProp.WIDE_BAR) line(p.handF + P(-4, 0), p.handN + P(4, 0), c.equipment, 2f)
        cables(ill.scene, p)
        if (ill.weightAtHip) hipWeight(ill, p)
    }

    private fun handProp(ill: Illustration, hand: Pt, elbow: Pt, far: Boolean) {
        if (ill.weightAtHip) return
        val col = if (far) c.equipment.copy(alpha = 0.55f) else c.equipment
        when (ill.hand) {
            HandProp.DUMBBELL -> {
                // Bar runs along the forearm's perpendicular so it reads as gripped.
                val ang = atan2(hand.y - elbow.y, hand.x - elbow.x) + (Math.PI / 2).toFloat()
                val dx = cos(ang) * 4.5f
                val dy = sin(ang) * 4.5f
                val a = P(hand.x - dx, hand.y - dy)
                val b = P(hand.x + dx, hand.y + dy)
                line(a, b, col, 1.6f)
                line(a.lerp(b, -0.12f), a.lerp(b, 0.22f), col, 4.2f)
                line(b.lerp(a, -0.12f), b.lerp(a, 0.22f), col, 4.2f)
            }
            HandProp.BARBELL -> if (!far) {
                circle(hand, 7.5f, col)
                circle(hand, 2f, c.accent)
            }
            HandProp.KETTLEBELL -> if (!far) {
                circle(hand + P(0, 4.5), 4f, col)
                scope.drawCircle(col, 2.2f * s, o(hand + P(0, 1)), style = Stroke(1.2f * s))
            }
            HandProp.HANDLE -> circle(hand, 2.2f, c.accent.copy(alpha = if (far) 0.5f else 1f))
            HandProp.WIDE_BAR, HandProp.NONE -> Unit
        }
    }

    private fun hipWeight(ill: Illustration, p: Pose) {
        val at = p.hip + P(0, -4)
        when (ill.hand) {
            HandProp.BARBELL -> { circle(at, 7.5f, c.equipment); circle(at, 2f, c.accent) }
            else -> { line(at + P(-4, 0), at + P(4, 0), c.equipment, 1.6f); circle(at + P(-4, 0), 2.3f, c.equipment); circle(at + P(4, 0), 2.3f, c.equipment) }
        }
    }

    private fun footPlate(p: Pose) {
        // Plate perpendicular to the shin, i.e. along the sled direction.
        val dir = atan2(p.footN.y - p.kneeN.y, p.footN.x - p.kneeN.x) + (Math.PI / 2).toFloat()
        val dx = cos(dir) * 9f
        val dy = sin(dir) * 9f
        val f = p.footN + P(2, 0)
        line(P(f.x - dx, f.y - dy), P(f.x + dx, f.y + dy), c.equipment, 3f)
        // Rail back to the frame
        line(f, P(96, 30), c.equipment.copy(alpha = 0.35f), 1.5f)
    }

    private fun cables(scene: Scene, p: Pose) {
        val cable = c.accent.copy(alpha = 0.8f)
        when (scene) {
            Scene.CABLE_HIGH -> line(P(78, 8), p.handN, cable, 0.9f)
            Scene.CABLE_MID -> line(P(90, 24), p.handN, cable, 0.9f)
            Scene.CABLE_DUAL -> {
                line(P(11, 12), p.handF, cable, 0.9f)
                line(P(89, 12), p.handN, cable, 0.9f)
            }
            Scene.CABLE_PULLDOWN -> line(P(50, 1), p.handN.lerp(p.handF, 0.5f), cable, 0.9f)
            Scene.CABLE_ROW -> line(P(90, 74), p.handN, cable, 0.9f)
            Scene.ROWER -> line(P(84, 76), p.handN, cable, 0.9f)
            else -> Unit
        }
    }

    private fun drawScene(scene: Scene, p: Pose) {
        val eq = c.equipment
        val soft = c.equipment.copy(alpha = 0.45f)
        when (scene) {
            Scene.NONE -> Unit
            Scene.MAT -> rect(8, 90.5, 84, 2.2, c.accent.copy(alpha = 0.45f), 1f)
            Scene.BENCH -> {
                rect(22, 64, 58, 4.5, eq)
                rect(28, 68, 3, 24, soft, 0.5f); rect(70, 68, 3, 24, soft, 0.5f)
            }
            Scene.LOW_BENCH -> {
                rect(6, 63, 26, 5, eq)
                rect(10, 68, 3, 24, soft, 0.5f); rect(26, 68, 3, 24, soft, 0.5f)
            }
            Scene.BENCH_TOP -> rect(42, 14, 16, 46, soft, 3f)
            Scene.INCLINE_BENCH -> {
                rect(28, 66, 22, 4.5, eq)
                val path = Path().apply {
                    moveTo(o(44, 68).x, o(44, 68).y); lineTo(o(58, 32).x, o(58, 32).y)
                    lineTo(o(54, 30).x, o(54, 30).y); lineTo(o(39, 66).x, o(39, 66).y); close()
                }
                scope.drawPath(path, eq)
                rect(32, 70, 3, 22, soft, 0.5f); rect(46, 70, 3, 22, soft, 0.5f)
            }
            Scene.SEAT -> {
                rect(30, 64, 26, 4.5, eq)
                rect(30, 30, 5, 36, eq)
                rect(40, 68, 4, 24, soft, 0.5f)
            }
            Scene.SEAT_FRONT -> {
                rect(34, 26, 32, 38, soft, 3f) // back pad behind the figure
                rect(32, 64, 36, 5, eq)
                rect(48, 69, 4, 23, soft, 0.5f)
            }
            Scene.PULLUP_BAR -> {
                line(P(14, 6), P(86, 6), eq, 2.2f)
                line(P(16, 6), P(16, 92), soft, 2f); line(P(84, 6), P(84, 92), soft, 2f)
            }
            Scene.CABLE_HIGH -> {
                rect(80, 4, 6, 88, soft, 1f)
                circle(P(78, 8), 2f, eq)
            }
            Scene.CABLE_MID -> {
                rect(91, 4, 6, 88, soft, 1f)
                circle(P(90, 24), 2f, eq)
            }
            Scene.CABLE_DUAL -> {
                rect(4, 6, 5, 86, soft, 1f); rect(91, 6, 5, 86, soft, 1f)
                circle(P(11, 12), 2f, eq); circle(P(89, 12), 2f, eq)
            }
            Scene.CABLE_PULLDOWN -> {
                rect(34, 26, 32, 38, soft, 3f)
                rect(32, 64, 36, 5, eq)
                rect(48, 69, 4, 23, soft, 0.5f)
                rect(36, 50, 28, 4, eq) // thigh pad
                circle(P(50, 1), 2f, eq)
            }
            Scene.CABLE_ROW -> {
                rect(18, 74, 34, 4, eq)
                rect(70, 64, 4, 18, eq) // foot plate
                rect(92, 30, 5, 62, soft, 1f)
                circle(P(90, 74), 2f, eq)
            }
            Scene.LEG_PRESS -> {
                val path = Path().apply {
                    moveTo(o(10, 46).x, o(10, 46).y); lineTo(o(16, 42).x, o(16, 42).y)
                    lineTo(o(36, 72).x, o(36, 72).y); lineTo(o(50, 72).x, o(50, 72).y)
                    lineTo(o(50, 77).x, o(50, 77).y); lineTo(o(32, 77).x, o(32, 77).y); close()
                }
                scope.drawPath(path, eq)
                rect(38, 77, 4, 15, soft, 0.5f)
            }
            Scene.TREADMILL -> {
                rect(16, 87, 70, 5, eq, 2.5f)
                line(P(82, 87), P(88, 46), soft, 2.2f)
                rect(82, 42, 12, 5, eq)
            }
            Scene.ROWER -> {
                rect(8, 82, 80, 3, soft, 1f)
                rect(p.hip.x - 6, 79, 12, 3.5f, eq)
                circle(P(86, 76), 7f, soft)
                rect(56, 74, 4, 10, eq) // foot stretcher
            }
            Scene.BOX -> rect(16, 62, 18, 30, soft, 2f)
            Scene.STEP -> rect(42, 88, 22, 4.5, soft, 1f)
        }
    }
}
