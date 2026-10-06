package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import eu.kanade.presentation.more.stats.recap.motion.ambientSwing
import eu.kanade.presentation.more.stats.recap.palette.RecapPalette

/**
 * What a page is drawn over: its gradient, with soft glows in its colours drifting across it on loops of 17 to 53
 * seconds. It's laid out in page units over [page] and carries on past the page's edges, so the story fills the screen
 * with the same background a shared page gets.
 *
 * @param millis how long the glows have drifted; at 0 they're at rest.
 */
internal fun DrawScope.drawRecapBackdrop(palette: RecapPalette, millis: Long, page: Rect) {
    drawRect(
        Brush.verticalGradient(
            listOf(palette.background, palette.backgroundEnd),
            startY = page.top,
            endY = page.bottom,
        ),
    )
    val unit = page.width / PAGE_WIDTH.value
    Glows.forEach { glow ->
        val center = page.topLeft + Offset(
            glow.x + glow.driftX * ambientSwing(millis, glow.periodX),
            glow.y + glow.driftY * ambientSwing(millis, glow.periodY),
        ) * unit
        val radius = glow.radius * (1f + BREATH * ambientSwing(millis, glow.periodBreath)) * unit
        val color = glow.color(palette).copy(alpha = glow.alpha)
        drawCircle(
            brush = Brush.radialGradient(
                0f to color,
                0.45f to color.copy(alpha = glow.alpha * 0.55f),
                1f to Color.Transparent,
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

/** One glow: where it rests in page units, how far and how slowly it wanders from there. */
private class Glow(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val driftX: Float,
    val driftY: Float,
    val periodX: Long,
    val periodY: Long,
    val periodBreath: Long,
    val color: (RecapPalette) -> Color,
)

private val Glows = listOf(
    Glow(
        x = 70f, y = 290f, radius = 230f, alpha = 0.3f,
        driftX = 110f, driftY = 150f, periodX = 19_000L, periodY = 29_000L, periodBreath = 23_000L,
        color = { it.accent },
    ),
    Glow(
        x = 300f, y = 120f, radius = 220f, alpha = 0.85f,
        driftX = 120f, driftY = 160f, periodX = 37_000L, periodY = 53_000L, periodBreath = 31_000L,
        color = { it.faint },
    ),
    Glow(
        x = 220f, y = 560f, radius = 160f, alpha = 0.35f,
        driftX = 100f, driftY = 80f, periodX = 17_000L, periodY = 41_000L, periodBreath = 27_000L,
        color = { lerp(it.accent, it.backgroundEnd, 0.45f) },
    ),
)

/** How much a glow grows and shrinks as it drifts. */
private const val BREATH = 0.12f
