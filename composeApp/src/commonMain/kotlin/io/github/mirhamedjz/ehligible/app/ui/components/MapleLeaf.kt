package io.github.mirhamedjz.ehligible.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.mirhamedjz.ehligible.app.ui.theme.Brand
import io.github.mirhamedjz.ehligible.core.brand.LeafShape
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

internal const val LEAF_TILT_DEGREES = LeafShape.TILT_DEGREES

/** The leaf outline scaled to fit a square of [side] pixels with its top-left corner at [origin]. */
internal fun leafPath(origin: Offset, side: Float): Path = Path().apply {
    val k = side / 100f
    LeafShape.points.forEachIndexed { i, (x, y) ->
        val px = origin.x + x * k
        val py = origin.y + y * k
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

/**
 * A maple leaf that fills with red from the bottom as [progress] (0..1) grows, inside a countdown ring.
 * The fill line ripples gently; at 100 % the ring turns gold.
 */
@Composable
fun MapleLeafProgress(
    progress: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val brand = Brand.colors
    val ringTrack = MaterialTheme.colorScheme.surfaceVariant
    val fill = remember { Animatable(if (animate) 0f else progress) }
    LaunchedEffect(progress) { fill.animateTo(progress.coerceIn(0f, 1f), tween(durationMillis = 1_400)) }
    val wave by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2_600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        val side = min(size.width, size.height)
        val center = Offset(size.width / 2, size.height / 2)
        val ringStroke = side * 0.045f
        val ringRadius = side / 2 - ringStroke
        val done = fill.value >= 1f
        drawRing(center, ringRadius, ringStroke, ringTrack, if (done) brand.gold else brand.maple, fill.value)

        val leafSide = ringRadius * 1.45f
        val origin = Offset(center.x - leafSide / 2, center.y - leafSide * 0.52f)
        val leaf = leafPath(origin, leafSide)
        val joint = Stroke(width = side * LeafShape.JOIN_FRACTION, join = StrokeJoin.Round, cap = StrokeCap.Round)
        rotate(LEAF_TILT_DEGREES, center) {
            drawPath(leaf, brand.leafEmpty)
            drawPath(leaf, brand.leafEmpty, style = joint)
            clipPath(leaf) {
                drawWaterLine(origin, leafSide, fill.value, wave, brand.maple)
            }
            if (fill.value > 0f) {
                // Round the filled leaf's tips too, but only below the water line.
                clipPath(waterBody(origin, leafSide, fill.value, wave)) { drawPath(leaf, brand.maple, style = joint) }
            }
        }
    }
}

private fun DrawScope.drawRing(
    center: Offset,
    radius: Float,
    stroke: Float,
    track: Color,
    color: Color,
    progress: Float,
) {
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2, radius * 2)
    drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
    if (progress > 0f) {
        drawArc(
            color,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

/** The filled part of the leaf: everything below a gently waving line at height [level]. */
private fun waterBody(origin: Offset, side: Float, level: Float, phase: Float): Path = Path().apply {
    // Leave a margin so the stroke-rounded tips stay inside the clipped area.
    val left = origin.x - side * 0.1f
    val right = origin.x + side * 1.1f
    val bottom = origin.y + side * 1.1f
    val baseY = origin.y + side * (1f - level * 0.97f) - side * 0.01f
    val amplitude = if (level in 0.01f..0.99f) side * 0.018f else 0f
    moveTo(left, bottom)
    lineTo(left, baseY)
    val steps = 24
    for (i in 0..steps) {
        val x = left + (right - left) * i / steps
        val y = baseY + amplitude * sin(phase + i * 0.55f)
        lineTo(x, y)
    }
    lineTo(right, bottom)
    close()
}

private fun DrawScope.drawWaterLine(origin: Offset, side: Float, level: Float, phase: Float, color: Color) {
    if (level <= 0f) return
    drawPath(waterBody(origin, side, level, phase), color)
}
