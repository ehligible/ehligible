package io.github.mirhamedjz.ehligible.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import androidx.core.graphics.withRotation
import io.github.mirhamedjz.ehligible.core.brand.LeafShape

/** Draws the filling leaf inside its countdown ring as a bitmap (widgets can't run Compose canvases). */
object LeafBitmap {
    fun render(sizePx: Int, progress: Float, filled: Int, empty: Int, ring: Int, ringTrack: Int): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        val side = sizePx.toFloat()
        val center = side / 2
        val ringStroke = side * 0.06f
        val radius = side / 2 - ringStroke
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = ringStroke
        paint.strokeCap = Paint.Cap.ROUND
        val arc = RectF(center - radius, center - radius, center + radius, center + radius)
        paint.color = ringTrack
        canvas.drawArc(arc, 0f, 360f, false, paint)
        if (progress > 0f) {
            paint.color = ring
            canvas.drawArc(arc, -90f, 360f * progress.coerceAtMost(1f), false, paint)
        }

        val leafSide = radius * 1.45f
        val left = center - leafSide / 2
        val top = center - leafSide * 0.52f
        val leaf = Path().apply {
            LeafShape.points.forEachIndexed { i, (x, y) ->
                val px = left + x * leafSide / 100f
                val py = top + y * leafSide / 100f
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        canvas.withRotation(LeafShape.TILT_DEGREES, center, center) {
            drawLeaf(this, leaf, empty, side)
            if (progress > 0f) {
                val waterY = top + leafSide * (1f - progress.coerceIn(0f, 1f) * 0.97f) - leafSide * 0.01f
                withClip(RectF(0f, waterY, side, side)) { drawLeaf(this, leaf, filled, side) }
            }
        }
        return bitmap
    }

    private fun drawLeaf(canvas: Canvas, leaf: Path, color: Int, side: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = side * LeafShape.JOIN_FRACTION
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(leaf, paint)
    }
}
