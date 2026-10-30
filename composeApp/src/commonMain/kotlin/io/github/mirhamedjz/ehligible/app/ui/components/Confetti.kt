package io.github.mirhamedjz.ehligible.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import io.github.mirhamedjz.ehligible.app.ui.theme.Brand
import kotlin.math.sin
import kotlin.random.Random

private class Piece(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val sway: Float,
    val spin: Float,
    val size: Float,
    val color: Color,
    val isLeaf: Boolean,
)

/**
 * A short burst of confetti (and the odd tiny maple leaf) falling over the screen.
 * Changing [key] replays it; [onFinished] fires when the last piece has left the screen.
 */
@Composable
fun ConfettiBurst(key: Any, onFinished: () -> Unit, modifier: Modifier = Modifier, durationMillis: Int = 3_200) {
    val palette = Brand.colors.confetti
    val pieces = remember(key) {
        val random = Random(key.hashCode())
        List(90) {
            Piece(
                x = random.nextFloat(),
                delay = random.nextFloat() * 0.35f,
                speed = 0.55f + random.nextFloat() * 0.6f,
                sway = 0.02f + random.nextFloat() * 0.05f,
                spin = 180f + random.nextFloat() * 540f,
                size = 6f + random.nextFloat() * 8f,
                color = palette[random.nextInt(palette.size)],
                isLeaf = random.nextFloat() < 0.18f,
            )
        }
    }
    var elapsedNanos by remember(key) { mutableLongStateOf(0L) }
    LaunchedEffect(key) {
        val start = withFrameNanos { it }
        while (elapsedNanos < durationMillis * 1_000_000L) {
            elapsedNanos = withFrameNanos { it } - start
        }
        onFinished()
    }
    Canvas(modifier.fillMaxSize()) {
        val t = elapsedNanos / (durationMillis * 1_000_000f)
        for (piece in pieces) {
            val progress = ((t - piece.delay) * piece.speed * 1.6f).coerceAtLeast(0f)
            if (progress <= 0f || progress > 1.2f) continue
            val x = (piece.x + piece.sway * sin(progress * 12f + piece.x * 6f)) * size.width
            val y = -40f + progress * (size.height + 80f)
            val s = piece.size * density / 2f
            rotate(piece.spin * progress, Offset(x, y)) {
                if (piece.isLeaf) {
                    drawPath(leafPath(Offset(x - s * 1.3f, y - s * 1.3f), s * 2.6f), Color(0xFFD62828))
                } else {
                    drawRect(piece.color, topLeft = Offset(x - s / 2, y - s / 4), size = Size(s, s / 2))
                }
            }
        }
    }
}
