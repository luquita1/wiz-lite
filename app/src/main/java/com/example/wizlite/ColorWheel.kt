package com.example.wizlite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Rueda de color: ángulo = tono (0-360), distancia al centro = saturación (0-1).
 * onChange se llama mientras arrastras; onFinish al soltar.
 */
@Composable
fun ColorWheel(
    hue: Float,
    sat: Float,
    onChange: (Float, Float) -> Unit,
    onFinish: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var box by remember { mutableStateOf(IntSize.Zero) }
    val changeCb by rememberUpdatedState(onChange)
    val finishCb by rememberUpdatedState(onFinish)
    var lastH by remember { mutableFloatStateOf(hue) }
    var lastS by remember { mutableFloatStateOf(sat) }

    fun update(pos: Offset) {
        val cx = box.width / 2f
        val cy = box.height / 2f
        val r = min(cx, cy)
        if (r <= 0f) return
        val dx = pos.x - cx
        val dy = pos.y - cy
        var h = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        if (h < 0f) h += 360f
        lastH = h
        lastS = (hypot(dx, dy) / r).coerceIn(0f, 1f)
        changeCb(lastH, lastS)
    }

    Canvas(
        modifier
            .aspectRatio(1f)
            .onSizeChanged { box = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { update(it) },
                    onDragEnd = { finishCb(lastH, lastS) },
                    onDragCancel = { finishCb(lastH, lastS) },
                    onDrag = { pc, _ -> update(pc.position) }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { update(it); finishCb(lastH, lastS) })
            }
    ) {
        val c = center
        val r = this.size.minDimension / 2f
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                c
            ),
            radius = r, center = c
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White, Color.Transparent), c, r),
            radius = r, center = c
        )
        val a = Math.toRadians(hue.toDouble())
        val p = Offset(c.x + cos(a).toFloat() * sat * r, c.y + sin(a).toFloat() * sat * r)
        drawCircle(Color.hsv(hue, sat, 1f), 13.dp.toPx(), p)
        drawCircle(Color.White, 13.dp.toPx(), p, style = Stroke(3.dp.toPx()))
        drawCircle(Color.Black.copy(alpha = 0.35f), 16.dp.toPx(), p, style = Stroke(1.dp.toPx()))
    }
}
