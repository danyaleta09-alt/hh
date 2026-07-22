package com.letify.app.ui.water

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letify.app.ui.theme.Letify
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated water-fill circle.
 *
 * — No ring border. A single smooth disc that fills from the bottom up,
 *   with a subtle animated wave on the fill surface for a liquid feel.
 * — Track disc is the container color. Fill color is the accent blue.
 * — Center text shows current / goal.
 *
 * @param progress    fill level in [0, 1]
 * @param currentMl   text label for the numerator
 * @param goalMl      text label for the denominator
 * @param size        outer diameter of the circle
 * @param fillColor   color of the water fill (defaults to #3FA8F5)
 * @param trackColor  color of the empty part (defaults to container)
 */
@Composable
fun WaterFillCircle(
    progress: Float,
    currentMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    fillColor: Color = Color(0xFF3FA8F5),
    trackColor: Color = Letify.colors.container,
) {
    val safeProgress = progress.coerceIn(0f, 1f)

    // Animate the fill level
    val animFill = remember { Animatable(safeProgress) }
    LaunchedEffect(safeProgress) {
        animFill.animateTo(safeProgress, tween(900, easing = EaseInOutCubic))
    }

    // Animate a wave phase for the surface ripple
    val wavePhase = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            wavePhase.animateTo(
                2f * PI.toFloat(),
                animationSpec = tween(3000, easing = androidx.compose.animation.core.LinearEasing),
            )
            wavePhase.snapTo(0f)
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = cx.coerceAtMost(cy)

            // ── 1. Track disc ──────────────────────────────────────────────
            drawCircle(color = trackColor, radius = r, center = Offset(cx, cy))

            // ── 2. Water fill with wave clipped to circle ──────────────────
            val fillLevel = animFill.value
            // Y position of the water surface from the BOTTOM of the circle.
            // fillLevel = 0 → top of fill = bottom of circle (empty)
            // fillLevel = 1 → top of fill = top of circle (full)
            val waterTopY = cy + r - 2f * r * fillLevel

            // Wave amplitude scales down to 0 at empty and full so it
            // disappears cleanly at the extremes.
            val edgeFade = (fillLevel * (1f - fillLevel) * 4f).coerceIn(0f, 1f)
            val waveAmp = r * 0.04f * edgeFade
            val waveFreq = 2f * PI.toFloat() / (this.size.width)

            // Clip everything to the circle first
            val circlePath = Path().apply {
                addOval(Rect(center = Offset(cx, cy), radius = r))
            }

            clipPath(circlePath, clipOp = ClipOp.Intersect) {
                // Build a polygon: wave along the top edge, then down + back
                val fillPath = Path().apply {
                    // Start bottom-left
                    moveTo(0f, this@Canvas.size.height + 10f)
                    lineTo(this@Canvas.size.width + 10f, this@Canvas.size.height + 10f)
                    lineTo(this@Canvas.size.width + 10f, waterTopY)
                    // Wave across the top (right → left)
                    val steps = 60
                    val step = this@Canvas.size.width / steps
                    for (i in steps downTo 0) {
                        val x = i * step
                        val waveY = waterTopY +
                            waveAmp * sin(waveFreq * x + wavePhase.value) -
                            waveAmp * 0.4f * sin(2f * waveFreq * x + wavePhase.value + 1f)
                        lineTo(x, waveY)
                    }
                    close()
                }
                drawPath(fillPath, color = fillColor)
                // Subtle lighter top layer (foam effect)
                val foamPath = Path().apply {
                    val foamH = r * 0.025f * edgeFade
                    moveTo(0f, waterTopY + foamH * 2f)
                    lineTo(this@Canvas.size.width + 10f, waterTopY + foamH * 2f)
                    lineTo(this@Canvas.size.width + 10f, waterTopY)
                    val steps = 60
                    val step = this@Canvas.size.width / steps
                    for (i in steps downTo 0) {
                        val x = i * step
                        val waveY = waterTopY +
                            waveAmp * sin(waveFreq * x + wavePhase.value + 0.5f)
                        lineTo(x, waveY)
                    }
                    close()
                }
                drawPath(foamPath, color = fillColor.copy(alpha = 0.4f))
            }
        }

        // Center labels
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${currentMl}",
                color = if (progress > 0.45f) Color.White else Letify.colors.text,
                style = Letify.typography.displayMedium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "из ${goalMl} мл",
                color = if (progress > 0.45f)
                    Color.White.copy(alpha = 0.75f)
                else
                    Letify.colors.muted,
                style = Letify.typography.bodySmall,
            )
        }
    }
}
