package com.paperfly.paperplanedrift.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

private val PaperCream = Color(0xFFFFFDF4)
private val PaperEdge = Color(0xFFE8DFC8)
private val RuleBlue = Color(0xFF7FA8C9)
private val MarginRed = Color(0xFFE57373)
private val MetalGray = Color(0xFF9AA3AD)
private val MetalDark = Color(0xFF6B7075)
private val WoodBrown = Color(0xFFC89B6C)
private val SkyGlass = Color(0xFFD7EBFA)
private val GustBlue = Color(0xFF6E93B8)
private val ScrapColors = listOf(Color(0xFFFFFFFF), Color(0xFFF4ECD8), Color(0xFFE8DFC8))

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
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFEFAF1), Color(0xFFF8ECD9)),
        ),
        size = size,
    )

    // Far parallax layer: soft doodle clouds.
    val farOffset = state.distance * 0.16f * unit
    drawCloudLayer(farOffset, yFraction = 0.18f, scale = 1f, alpha = 0.55f)
    // Near parallax layer: slightly faster, lower clouds.
    val nearOffset = state.distance * 0.34f * unit
    drawCloudLayer(nearOffset, yFraction = 0.62f, scale = 1.35f, alpha = 0.35f)

    // Faint notebook rule lines.
    var y = 10f * unit
    while (y < size.height) {
        drawLine(
            color = RuleBlue.copy(alpha = 0.10f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = unit * 0.18f,
        )
        y += 9f * unit
    }
    // Red margin line.
    drawLine(
        color = MarginRed.copy(alpha = 0.14f),
        start = Offset(size.width * 0.08f, 0f),
        end = Offset(size.width * 0.08f, size.height),
        strokeWidth = unit * 0.25f,
    )
}

private fun DrawScope.drawCloudLayer(offset: Float, yFraction: Float, scale: Float, alpha: Float) {
    val span = size.width + 260f * scale
    for (i in 0 until 3) {
        val base = i * span / 3f
        val x = ((base - offset) % span + span) % span - 130f * scale
        val y = size.height * yFraction + (i - 1) * size.height * 0.09f
        val r = 34f * scale
        val cloud = Color.White.copy(alpha = alpha)
        drawCircle(cloud, radius = r, center = Offset(x, y))
        drawCircle(cloud, radius = r * 0.75f, center = Offset(x + r * 0.9f, y + r * 0.25f))
        drawCircle(cloud, radius = r * 0.7f, center = Offset(x - r * 0.9f, y + r * 0.28f))
    }
}

// ---------------------------------------------------------------- obstacles

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
        drawPath(path, PaperCream)
        drawPath(path, PaperEdge, style = Stroke(width = unit * 0.3f))
        // Rule lines on the paper strip.
        val from = if (isTop) 0f else yEdge + amp
        val to = if (isTop) yEdge - amp else size.height
        var y = from + unit * 4f
        while (y < to - unit) {
            drawLine(
                RuleBlue.copy(alpha = 0.35f),
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
    // Top blade points down at the gap, bottom blade points up.
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
    drawPath(topBlade, MetalGray)
    drawPath(bottomBlade, MetalGray)
    drawPath(topBlade, MetalDark, style = Stroke(width = unit * 0.3f))
    drawPath(bottomBlade, MetalDark, style = Stroke(width = unit * 0.3f))
    // Stylized handles at screen edges.
    drawCircle(Color(0xFFE1653F), radius = unit * 1.6f, center = Offset(x - hw * 0.15f, unit * 1.8f))
    drawCircle(Color(0xFFE1653F), radius = unit * 1.6f, center = Offset(x - hw * 0.15f, size.height - unit * 1.8f))
}

private fun DrawScope.drawStapler(x: Float, hw: Float, topEdge: Float, bottomEdge: Float, unit: Float) {
    // Top jaw: red body with a metal strike plate at the gap edge.
    drawRect(Color(0xFFB0413E), topLeft = Offset(x - hw, 0f), size = Size(hw * 2f, (topEdge - unit * 1.4f).coerceAtLeast(0f)))
    drawRect(MetalGray, topLeft = Offset(x - hw, (topEdge - unit * 1.4f).coerceAtLeast(0f)), size = Size(hw * 2f, unit * 1.4f))
    // Bottom anvil: metal strip on a gray base.
    drawRect(MetalGray, topLeft = Offset(x - hw, bottomEdge), size = Size(hw * 2f, unit * 1.4f))
    drawRect(MetalDark, topLeft = Offset(x - hw, bottomEdge + unit * 1.4f), size = Size(hw * 2f, (size.height - bottomEdge - unit * 1.4f).coerceAtLeast(0f)))
    // Staples along the strike plate.
    var sx = x - hw + unit
    while (sx < x + hw - unit) {
        drawRect(Color(0xFFE8EDF2), topLeft = Offset(sx, (topEdge - unit * 0.9f)), size = Size(unit * 0.8f, unit * 0.5f))
        sx += unit * 2f
    }
}

private fun DrawScope.drawWindow(x: Float, hw: Float, topEdge: Float, bottomEdge: Float, unit: Float) {
    fun framePart(from: Float, to: Float) {
        if (to - from <= 0f) return
        drawRect(WoodBrown, topLeft = Offset(x - hw, from), size = Size(hw * 2f, to - from))
        val inset = unit * 1.1f
        val glassHeight = to - from - inset * 2f
        if (glassHeight > 0f) {
            drawRect(SkyGlass, topLeft = Offset(x - hw + inset, from + inset), size = Size(hw * 2f - inset * 2f, glassHeight))
            // Muntin bar.
            drawLine(
                WoodBrown,
                Offset(x, from + inset),
                Offset(x, to - inset),
                strokeWidth = unit * 0.5f,
            )
        }
    }
    framePart(0f, topEdge)
    framePart(bottomEdge, size.height)
    // Sill edges facing the gap.
    drawRect(Color(0xFFA87F52), topLeft = Offset(x - hw, topEdge - unit * 0.7f), size = Size(hw * 2f, unit * 0.7f))
    drawRect(Color(0xFFA87F52), topLeft = Offset(x - hw, bottomEdge), size = Size(hw * 2f, unit * 0.7f))
}

private fun DrawScope.drawFan(x: Float, elapsed: Float, unit: Float) {
    val cy = size.height - 8f * unit
    // Stand.
    drawRect(MetalDark, topLeft = Offset(x - unit * 0.8f, cy), size = Size(unit * 1.6f, unit * 6f))
    drawRect(MetalDark, topLeft = Offset(x - unit * 3f, size.height - unit * 1.2f), size = Size(unit * 6f, unit * 1.2f))
    // Cage.
    drawCircle(MetalGray, radius = unit * 5f, center = Offset(x, cy))
    drawCircle(Color(0xFFCBD3DA), radius = unit * 4.3f, center = Offset(x, cy))
    // Spinning blades.
    val angle = elapsed * 520f
    withTransform({ rotate(angle, pivot = Offset(x, cy)) }) {
        for (i in 0 until 3) {
            withTransform({ rotate(i * 120f, pivot = Offset(x, cy)) }) {
                drawOval(
                    MetalDark.copy(alpha = 0.8f),
                    topLeft = Offset(x - unit * 0.9f, cy - unit * 4.1f),
                    size = Size(unit * 1.8f, unit * 3.6f),
                )
            }
        }
    }
    drawCircle(MetalDark, radius = unit * 0.9f, center = Offset(x, cy))
}

// ---------------------------------------------------------------- wind gusts

private fun DrawScope.drawGust(gust: WindGust, state: GameUiState, unit: Float) {
    val left = (gust.startX - state.distance) * unit
    val right = (gust.endX - state.distance) * unit
    if (right < 0f || left > size.width) return

    val up = gust.forceY < 0f
    val strengthT = (abs(gust.forceY) / GameConfig.MAX_GUST_STRENGTH).coerceIn(0f, 1f)
    val alpha = 0.16f + 0.16f * strengthT
    val color = GustBlue.copy(alpha = alpha)

    // Soft zone tint so the player can read the gust area.
    drawRect(
        color = GustBlue.copy(alpha = alpha * 0.25f),
        topLeft = Offset(left, 0f),
        size = Size(right - left, size.height),
    )

    val colSpacing = 7f * unit
    val rowSpacing = 12f * unit
    val drift = (state.elapsed * 26f * unit) % rowSpacing
    val chevronH = 2.2f * unit
    val chevronW = 2.0f * unit

    var cx = left + colSpacing / 2f
    while (cx < right) {
        var cyBase = -rowSpacing + (if (up) -drift else drift)
        var cy = cyBase
        while (cy < size.height + rowSpacing) {
            val tip = if (up) cy else cy + chevronH
            val tail = if (up) cy + chevronH else cy
            drawLine(color, Offset(cx - chevronW, tail), Offset(cx, tip), strokeWidth = unit * 0.45f)
            drawLine(color, Offset(cx + chevronW, tail), Offset(cx, tip), strokeWidth = unit * 0.45f)
            cy += rowSpacing
        }
        cx += colSpacing
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
            if (visible) drawPaperPlane(Offset(px, py), halfW, halfH, pitch, skin)
        }
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
    drawPath(path, Color(skin.shadeColor), style = Stroke(width = radius * 0.16f))
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
