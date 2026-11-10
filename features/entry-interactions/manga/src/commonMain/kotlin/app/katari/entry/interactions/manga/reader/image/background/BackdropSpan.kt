package app.katari.entry.interactions.manga.reader.image.background

import kotlin.math.roundToInt

/**
 * A stretch of the space beside a page from [top] to [bottom], as fractions of the page's height, running from
 * [topArgb] to [bottomArgb]; the two are the same where it holds one colour. ARGB without premultiplied alpha.
 */
internal data class BackdropSpan(val top: Float, val bottom: Float, val topArgb: Int, val bottomArgb: Int)

/**
 * The part of the span from [from] to [to], fractions of the same height, taken as the fraction of that part
 * [from]..[to] covers, so a fade cut in two keeps the colours it has at the cut. Null when the span misses the part.
 */
internal fun BackdropSpan.within(from: Float, to: Float): BackdropSpan? {
    val top = maxOf(top, from)
    val bottom = minOf(bottom, to)
    if (bottom <= top || to <= from) return null
    return BackdropSpan(
        top = (top - from) / (to - from),
        bottom = (bottom - from) / (to - from),
        topArgb = argbAt(top),
        bottomArgb = argbAt(bottom),
    )
}

private fun BackdropSpan.argbAt(position: Float): Int {
    if (topArgb == bottomArgb || bottom <= top) return topArgb
    val t = ((position - top) / (bottom - top)).coerceIn(0f, 1f)
    fun channel(shift: Int): Int {
        val from = (topArgb shr shift) and 0xFF
        val to = (bottomArgb shr shift) and 0xFF
        return (from + (to - from) * t).roundToInt() shl shift
    }
    return channel(24) or channel(16) or channel(8) or channel(0)
}
