package com.paperfly.paperplanedrift.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.paperfly.paperplanedrift.data.PlaneSkin
import com.paperfly.paperplanedrift.domain.GameConfig
import com.paperfly.paperplanedrift.domain.GamePhase
import com.paperfly.paperplanedrift.domain.Obstacle
import com.paperfly.paperplanedrift.domain.ObstacleType
import com.paperfly.paperplanedrift.domain.WindGust
import com.paperfly.paperplanedrift.ui.GameUiState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Craft Paper Minimal palette (design brief).
private val Cream = Color(0xFFF6EFE2)
private val Ink = Color(0xFF3A322A)
private val Tan = Color(0xFFA38F76)
private val Teal = Color(0xFF4F8C93)
private val PaperWhite = Color(0xFFFFFDF4)
private val Disabled = Color(0xFFD8CDBA)
private val Terracotta = Color(0xFFE2703A)
private val ScrapColors = listOf(PaperWhite, Cream, Disabled)

@Composable
fun GameCanvas(state: GameUiState, skin: PlaneSkin, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val unit = size.height / GameConfig.WORLD_HEIGHT

        drawBackground(state, unit)

        for (gust in state.gusts) drawGust(gust, state, unit)
        for (obstacle in state.obstacles) drawObstacle(obstacle, state, unit)

        drawPlaneOrCrumple(state, skin, unit)
        drawParticles(state, unit)
    }
}

// ---------------------------------------------------------------- background

private fun DrawScope.drawBackground(state: GameUiState, unit: Float) {
    // Flat kraft cream — the background reads as paper, not sky gradient.
    drawRect(Cream, size = size)

    // Far parallax layer: faint pencil-line doodle clouds.
    val farOffset = state.distance * 0.16f * unit
    drawCloudLayer(farOffset, yFraction = 0.18f, scale = 1f, alpha = 0.35f, unit = unit)
    val nearOffset = state.distance * 0.34f * unit
    drawCloudLayer(nearOffset, yFraction = 0.62f, scale = 1.35f, alpha = 0.22f, unit = unit)

    // Faint pencil rule lines on the paper.
    var y = 10f * unit
    while (y < size.height) {
        drawLine(
            color = Tan.copy(alpha = 0.14f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = unit * 0.16f,
        )
        y += 9f * unit
    }
}

private fun DrawScope.drawCloudLayer(offset: Float, yFraction: Float, scale: Float, alpha: Float, unit: Float) {
    val span = size.width + 260f * scale
    for (i in 0 until 3) {
        val base = i * span / 3f
        val x = ((base - offset) % span + span) % span - 130f * scale
        val y = size.height * yFraction + (i - 1) * size.height * 0.09f
        val r = 34f * scale
        val fill = PaperWhite.copy(alpha = alpha)
        drawCircle(fill, radius = r, center = Offset(x, y))
        drawCircle(fill, radius = r * 0.75f, center = Offset(x + r * 0.9f, y + r * 0.25f))
        drawCircle(fill, radius = r * 0.7f, center = Offset(x - r * 0.9f, y + r * 0.28f))
        // Pencil underline to keep clouds "drawn on the page".
        drawLine(
            Tan.copy(alpha = alpha * 0.9f),
            Offset(x - r * 1.5f, y + r * 0.85f),
            Offset(x + r * 1.5f, y + r * 0.85f),
            strokeWidth = unit * 0.16f,
            cap = StrokeCap.Round,
        )
    }
}

// ---------------------------------------------------------------- obstacles

private fun DrawScope.inkStroke(unit: Float) = Stroke(width = unit * 0.42f)

/** Tan drop shadow beneath a path for the "lifted off the page" feel. */
private fun DrawScope.drawLifted(path: Path, fill: Color, unit: Float) {
    withTransform({ translate(unit * 0.7f, unit * 0.7f) }) {
        drawPath(path, Tan.copy(alpha = 0.45f))
    }
    drawPath(path, fill)
    drawPath(path, Ink, style = inkStroke(unit))
}

private fun DrawScope.drawObstacle(obstacle: Obstacle, state: GameUiState, unit: Float) {
    val x = (obstacle.worldX - state.distance) * unit
    val hw = obstacle.halfWidth * unit
    if (x + hw < -20f || x - hw > size.width + 20f) return

    val gapHalf = obstacle.currentGapHalf(state.elapsed)
    val topEdge = (obstacle.gapCenter - gapHalf) * unit
    val bottomEdge = (obstacle.gapCenter + gapHalf) * unit

    when (obstacle.type) {
        ObstacleType.TORN_PAPER -> drawTornPaper(x, hw, topEdge, bottomEdge, obstacle.id, unit)
        ObstacleType.SCISSORS -> drawScissors(x, hw, topEdge, bottomEdge, unit)
        ObstacleType.STAPLER -> drawStapler(x, hw, topEdge, bottomEdge, unit)
        ObstacleType.WINDOW -> drawWindow(x, hw, topEdge, bottomEdge, unit)
        ObstacleType.FAN -> drawFan(x, state.elapsed, unit)
    }
}

private fun DrawScope.drawTornPaper(x: Float, hw: Float, topEdge: Float, bottomEdge: Float, id: Int, unit: Float) {
    fun strip(yEdge: Float, isTop: Boolean) {
        val path = Path()
        val teeth = 6
        val amp = unit * 1.1f
        if (isTop) {
            path.moveTo(x - hw, 0f)
            for (i in 0..teeth) {
                val px = x - hw + (2f * hw) * i / teeth
                val jag = if ((i + id) % 2 == 0) 0f else amp
                path.lineTo(px, yEdge - jag)
            }
            path.lineTo(x + hw, 0f)
        } else {
            path.moveTo(x - hw, size.height)
            for (i in 0..teeth) {
                val px = x - hw + (2f * hw) * i / teeth
                val jag = if ((i + id) % 2 == 0) 0f else amp
                path.lineTo(px, yEdge + jag)
            }
            path.lineTo(x + hw, size.height)
        }
        path.close()
        drawLifted(path, PaperWhite, unit)
        // Pencil rule lines on the strip.
        val from = if (isTop) 0f else yEdge + amp
        val to = if (isTop) yEdge - amp else size.height
        var y = from + unit * 4f
        while (y < to - unit) {
            drawLine(
                Tan.copy(alpha = 0.5f),
                Offset(x - hw + unit, y),
                Offset(x + hw - unit, y),
                strokeWidth = unit * 0.15f,
            )
            y += unit * 4f
        }
    }
    strip(topEdge, isTop = true)
    strip(bottomEdge, isTop = false)
}

private fun DrawScope.drawScissors(x: Float, hw: Float, topEdge: Float, bottomEdge: Float, unit: Float) {
    val topBlade = Path().apply {
        moveTo(x - hw * 0.6f, 0f)
        lineTo(x + hw * 0.35f, 0f)
        lineTo(x, topEdge)
        close()
    }
    val bottomBlade = Path().apply {
        moveTo(x - hw * 0.6f, size.height)
        lineTo(x + hw * 0.35f, size.height)
        lineTo(x, bottomEdge)
        close()
    }
    drawLifted(topBlade, Disabled, unit)
    drawLifted(bottomBlade, Disabled, unit)
    // Terracotta handle loops at the screen edges.
    for (cy in listOf(unit * 1.8f, size.height - unit * 1.8f)) {
        val c = Offset(x - hw * 0.15f, cy)
        drawCircle(Terracotta, radius = unit * 1.6f, center = c)
        drawCircle(Ink, radius = unit * 1.6f, center = c, style = inkStroke(unit))
        drawCircle(Cream, radius = unit * 0.7f, center = c)
    }
}

private fun DrawScope.drawStapler(x: Float, hw: Float, topEdge: Float, bottomEdge: Float, unit: Float) {
    fun block(top: Float, bottom: Float, fill: Color) {
        if (bottom - top <= 0f) return
        val path = Path().apply { addRect(androidx.compose.ui.geometry.Rect(x - hw, top, x + hw, bottom)) }
        drawLifted(path, fill, unit)
    }
    // Top jaw: tan body with a pale strike plate at the gap edge.
    val plateTop = (topEdge - unit * 1.4f).coerceAtLeast(0f)
    block(0f, plateTop, Tan)
    block(plateTop, topEdge, Disabled)
    // Bottom anvil: pale strip on a tan base.
    block(bottomEdge, bottomEdge + unit * 1.4f, Disabled)
    block(bottomEdge + unit * 1.4f, size.height, Tan)
    // Staples along the strike plate.
    var sx = x - hw + unit
    while (sx < x + hw - unit) {
        drawRect(PaperWhite, topLeft = Offset(sx, (topEdge - unit * 0.9f)), size = Size(unit * 0.8f, unit * 0.5f))
        sx += unit * 2f
    }
}

private fun DrawScope.drawWindow(x: Float, hw: Float, topEdge: Float, bottomEdge: Float, unit: Float) {
    fun framePart(from: Float, to: Float) {
        if (to - from <= 0f) return
        val frame = Path().apply { addRect(androidx.compose.ui.geometry.Rect(x - hw, from, x + hw, to)) }
        drawLifted(frame, Tan, unit)
        val inset = unit * 1.1f
        val glassHeight = to - from - inset * 2f
        if (glassHeight > 0f) {
            drawRect(Color(0xFFEAF3F7), topLeft = Offset(x - hw + inset, from + inset), size = Size(hw * 2f - inset * 2f, glassHeight))
            drawRect(
                Ink.copy(alpha = 0.6f),
                topLeft = Offset(x - hw + inset, from + inset),
                size = Size(hw * 2f - inset * 2f, glassHeight),
                style = Stroke(width = unit * 0.2f),
            )
            // Muntin bar.
            drawLine(Ink.copy(alpha = 0.6f), Offset(x, from + inset), Offset(x, to - inset), strokeWidth = unit * 0.35f)
        }
    }
    framePart(0f, topEdge)
    framePart(bottomEdge, size.height)
}

private fun DrawScope.drawFan(x: Float, elapsed: Float, unit: Float) {
    val cy = size.height - 8f * unit
    // Stand.
    drawRect(Ink, topLeft = Offset(x - unit * 0.8f, cy), size = Size(unit * 1.6f, unit * 6f))
    drawRect(Ink, topLeft = Offset(x - unit * 3f, size.height - unit * 1.2f), size = Size(unit * 6f, unit * 1.2f))
    // Cage with tan lift shadow.
    drawCircle(Tan.copy(alpha = 0.45f), radius = unit * 5f, center = Offset(x + unit * 0.7f, cy + unit * 0.7f))
    drawCircle(Disabled, radius = unit * 5f, center = Offset(x, cy))
    drawCircle(Ink, radius = unit * 5f, center = Offset(x, cy), style = inkStroke(unit))
    drawCircle(Cream, radius = unit * 4.2f, center = Offset(x, cy))
    // Spinning blades.
    val angle = elapsed * 520f
    withTransform({ rotate(angle, pivot = Offset(x, cy)) }) {
        for (i in 0 until 3) {
            withTransform({ rotate(i * 120f, pivot = Offset(x, cy)) }) {
                drawOval(
                    Tan,
                    topLeft = Offset(x - unit * 0.9f, cy - unit * 4.1f),
                    size = Size(unit * 1.8f, unit * 3.6f),
                )
                drawOval(
                    Ink,
                    topLeft = Offset(x - unit * 0.9f, cy - unit * 4.1f),
                    size = Size(unit * 1.8f, unit * 3.6f),
                    style = Stroke(width = unit * 0.25f),
                )
            }
        }
    }
    drawCircle(Ink, radius = unit * 0.9f, center = Offset(x, cy))
}

// ---------------------------------------------------------------- wind gusts

/**
 * Dusty-teal treatment from the brief: translucent curved streaks with animated
 * dash-offset for continuous flow, arrow cap on the leading streak.
 */
private fun DrawScope.drawGust(gust: WindGust, state: GameUiState, unit: Float) {
    val left = (gust.startX - state.distance) * unit
    val right = (gust.endX - state.distance) * unit
    if (right < 0f || left > size.width) return

    val up = gust.forceY < 0f
    val strengthT = (abs(gust.forceY) / GameConfig.MAX_GUST_STRENGTH).coerceIn(0f, 1f)
    val streakAlpha = 0.30f + 0.18f * strengthT

    // Soft zone tint so the gust area is readable.
    drawRect(
        color = Teal.copy(alpha = 0.05f + 0.05f * strengthT),
        topLeft = Offset(left, 0f),
        size = Size(right - left, size.height),
    )

    val streaks = 4
    val width = right - left
    val flow = state.elapsed * 40f * unit
    val dash = PathEffect.dashPathEffect(
        floatArrayOf(6f * unit, 3.2f * unit),
        phase = if (up) flow else -flow,
    )
    val stroke = Stroke(width = unit * 0.5f, cap = StrokeCap.Round, pathEffect = dash)
    val amp = 1.6f * unit

    for (i in 0 until streaks) {
        val cx = left + width * (i + 0.5f) / streaks
        val phase = i * 1.7f
        val path = Path()
        var y = -4f * unit
        path.moveTo(cx + sin(phase) * amp, y)
        while (y < size.height + 4f * unit) {
            y += 3f * unit
            path.lineTo(cx + sin(y / (9f * unit) + phase) * amp, y)
        }
        drawPath(path, Teal.copy(alpha = streakAlpha), style = stroke)

        // Arrow cap on the leading streak, pointing with the flow.
        if (i == 0) {
            val tipY = if (up) 6f * unit else size.height - 6f * unit
            val tipX = cx + sin(tipY / (9f * unit) + phase) * amp
            val dir = if (up) -1f else 1f
            val solid = Stroke(width = unit * 0.55f, cap = StrokeCap.Round)
            drawLine(
                Teal.copy(alpha = streakAlpha + 0.2f),
                Offset(tipX - 1.4f * unit, tipY - dir * 2.2f * unit),
                Offset(tipX, tipY),
                strokeWidth = solid.width, cap = StrokeCap.Round,
            )
            drawLine(
                Teal.copy(alpha = streakAlpha + 0.2f),
                Offset(tipX + 1.4f * unit, tipY - dir * 2.2f * unit),
                Offset(tipX, tipY),
                strokeWidth = solid.width, cap = StrokeCap.Round,
            )
        }
    }
}

// ---------------------------------------------------------------- plane

/**
 * Shared paper-plane renderer, also used by menu/shop previews.
 * [halfW]/[halfH] are in pixels; the plane is drawn centered on [center].
 */
fun DrawScope.drawPaperPlane(
    center: Offset,
    halfW: Float,
    halfH: Float,
    pitchDeg: Float,
    skin: PlaneSkin,
    scale: Float = 1f,
) {
    withTransform({
        translate(center.x, center.y)
        rotate(pitchDeg, pivot = Offset.Zero)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        val body = Path().apply {
            moveTo(halfW, 0f)
            lineTo(-halfW, -halfH)
            lineTo(-halfW * 0.4f, 0f)
            lineTo(-halfW, halfH * 0.85f)
            close()
        }
        val fold = Path().apply {
            moveTo(halfW, 0f)
            lineTo(-halfW * 0.4f, 0f)
            lineTo(-halfW, halfH * 0.85f)
            close()
        }
        // Tan lift shadow under the plane.
        withTransform({ translate(halfH * 0.18f, halfH * 0.22f) }) {
            drawPath(body, Tan.copy(alpha = 0.35f))
        }
        drawPath(body, Color(skin.bodyColor))
        drawPath(fold, Color(skin.shadeColor))
        drawPath(body, Color(skin.accentColor), style = Stroke(width = halfH * 0.14f))
        drawLine(
            Color(skin.accentColor).copy(alpha = 0.6f),
            Offset(halfW, 0f),
            Offset(-halfW * 0.4f, 0f),
            strokeWidth = halfH * 0.10f,
        )
    }
}

private fun DrawScope.drawPlaneOrCrumple(state: GameUiState, skin: PlaneSkin, unit: Float) {
    val px = size.width * GameConfig.PLANE_X_FRACTION
    val py = state.plane.y * unit
    val halfW = GameConfig.PLANE_HALF_W * unit
    val halfH = GameConfig.PLANE_HALF_H * unit

    when (state.phase) {
        GamePhase.CRASHING, GamePhase.GAME_OVER -> {
            val t = (state.crashTimer / GameConfig.CRASH_ANIM_SECONDS).coerceIn(0f, 1f)
            if (t < 0.45f) {
                // Spin + shrink into a ball.
                drawPaperPlane(Offset(px, py), halfW, halfH, pitchDeg = t * 700f, skin = skin, scale = 1f - t)
            } else {
                drawCrumpleBall(Offset(px, py), radius = halfH * 1.25f, skin = skin)
                // Small ink impact lines radiating outward.
                if (t < 0.85f) {
                    val r0 = halfH * 1.6f
                    val len = halfH * (0.7f + t)
                    for (a in listOf(-140f, -95f, -40f)) {
                        val rad = a * Math.PI.toFloat() / 180f
                        drawLine(
                            Ink.copy(alpha = (1f - t)),
                            Offset(px + cos(rad) * r0, py + sin(rad) * r0),
                            Offset(px + cos(rad) * (r0 + len), py + sin(rad) * (r0 + len)),
                            strokeWidth = unit * 0.35f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
        else -> {
            var pitch = (state.plane.vy * 0.45f).coerceIn(-26f, 42f)
            if (state.phase == GamePhase.READY) pitch = sin(state.elapsed * 2.2f) * 6f
            // Slight bank wobble while inside a gust, so wind is readable on the plane itself.
            val inGust = state.gusts.any { it.contains(state.planeWorldX) }
            if (inGust) pitch += sin(state.elapsed * 18f) * 4f
            // Blink while invulnerable after a revive.
            val visible = state.invulnerableFor <= 0f || (state.elapsed * 8f).toInt() % 2 == 0
            if (visible) {
                if (state.phase == GamePhase.RUNNING) {
                    drawMotionTrail(px, py, halfW, halfH, pitch, state.plane.vy, unit)
                }
                drawPaperPlane(Offset(px, py), halfW, halfH, pitch, skin)
            }
        }
    }
}

/** Short 2-segment motion trail behind the tail; stretches longer while diving. */
private fun DrawScope.drawMotionTrail(px: Float, py: Float, halfW: Float, halfH: Float, pitchDeg: Float, vy: Float, unit: Float) {
    val rad = pitchDeg * Math.PI.toFloat() / 180f
    val dirX = -cos(rad)
    val dirY = -sin(rad)
    val stretch = 1f + (vy / 60f).coerceIn(0f, 1f) * 0.9f
    for ((idx, off) in listOf(1.25f, 1.85f).withIndex()) {
        val startD = halfW * off
        val len = halfW * 0.55f * stretch
        val yOff = (idx * 2 - 1) * halfH * 0.35f
        drawLine(
            Tan.copy(alpha = 0.5f - idx * 0.15f),
            Offset(px + dirX * startD, py + dirY * startD + yOff),
            Offset(px + dirX * (startD + len), py + dirY * (startD + len) + yOff),
            strokeWidth = unit * 0.4f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawCrumpleBall(center: Offset, radius: Float, skin: PlaneSkin) {
    val path = Path()
    val points = 9
    for (i in 0 until points) {
        val angle = i * (2f * Math.PI.toFloat() / points)
        val r = radius * (0.75f + 0.3f * abs(sin(i * 2.7f)))
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, Color(skin.bodyColor))
    drawPath(path, Ink, style = Stroke(width = radius * 0.14f))
}

// ---------------------------------------------------------------- particles

private fun DrawScope.drawParticles(state: GameUiState, unit: Float) {
    for (p in state.particles) {
        val sx = (p.x - state.distance) * unit
        val sy = p.y * unit
        val s = p.size * unit
        withTransform({
            translate(sx, sy)
            rotate(p.rotation, pivot = Offset.Zero)
        }) {
            drawRect(
                ScrapColors[p.colorIndex % ScrapColors.size].copy(alpha = p.life.coerceIn(0f, 1f)),
                topLeft = Offset(-s / 2f, -s / 2f),
                size = Size(s, s * 0.7f),
            )
        }
    }
}
