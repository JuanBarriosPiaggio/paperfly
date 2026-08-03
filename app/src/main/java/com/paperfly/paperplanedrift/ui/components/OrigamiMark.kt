package com.paperfly.paperplanedrift.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * The Origami Facets brand mark: low-poly folded plane, nose-on,
 * four crease facets in tonal terracotta steps (matches the launcher icon).
 */
@Composable
fun OrigamiMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        fun p(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(x / 100f * w, y / 100f * h)

        fun facet(color: Color, a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>) {
            val path = Path().apply {
                val pa = p(a.first, a.second); val pb = p(b.first, b.second); val pc = p(c.first, c.second)
                moveTo(pa.x, pa.y); lineTo(pb.x, pb.y); lineTo(pc.x, pc.y); close()
            }
            drawPath(path, color)
        }

        facet(Color(0xFFE2703A), 50f to 10f, 90f to 55f, 50f to 45f)
        facet(Color(0xFFC95F2E), 50f to 10f, 10f to 55f, 50f to 45f)
        facet(Color(0xFFF0946B), 50f to 45f, 90f to 55f, 60f to 90f)
        facet(Color(0xFFA54E26), 50f to 45f, 10f to 55f, 40f to 90f)
    }
}
