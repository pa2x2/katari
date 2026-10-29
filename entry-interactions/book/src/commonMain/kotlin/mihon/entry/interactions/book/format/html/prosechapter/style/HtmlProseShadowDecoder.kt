package mihon.entry.interactions.book.format.html.prosechapter.style

import mihon.book.api.document.BookDocumentShadow
import mihon.entry.interactions.book.format.html.prosechapter.css.cssComponents
import mihon.entry.interactions.book.format.html.prosechapter.css.isTransparentColor
import mihon.entry.interactions.book.format.html.prosechapter.css.parseCssColor
import mihon.entry.interactions.book.format.html.prosechapter.css.parseCssLength
import mihon.entry.interactions.book.format.html.prosechapter.css.splitTopLevel

/** Decodes `box-shadow`; later layers are drawn under earlier ones, so the first [limit] are the visible ones. */
internal fun String?.toBoxShadows(limit: Int): List<BookDocumentShadow> =
    this?.takeUnless { it.trim().equals("none", ignoreCase = true) }
        ?.splitTopLevel(',')
        ?.mapNotNull { layer -> layer.toShadow(allowInset = true) }
        ?.take(limit)
        .orEmpty()

/** Decodes the topmost `text-shadow` layer. */
internal fun String?.toTextShadow(): BookDocumentShadow? =
    this?.takeUnless { it.trim().equals("none", ignoreCase = true) }
        ?.splitTopLevel(',')
        ?.firstNotNullOfOrNull { layer -> layer.toShadow(allowInset = false) }

private fun String.toShadow(allowInset: Boolean): BookDocumentShadow? {
    var inset = false
    var color: Long? = null
    val lengths = mutableListOf<Float>()
    cssComponents().forEach { component ->
        val lower = component.lowercase()
        when {
            lower == "inset" -> if (allowInset) inset = true else return null
            lower == "currentcolor" -> color = null
            else -> parseCssLength(lower)?.toDp()?.let(lengths::add)
                ?: parseCssColor(lower)?.let { color = it }
                ?: return null
        }
    }
    if (lengths.size !in 2..(if (allowInset) 4 else 3)) return null
    if (color?.isTransparentColor() == true) return null
    return BookDocumentShadow(
        offsetXDp = lengths[0].coerceIn(-MAX_SHADOW_DP, MAX_SHADOW_DP),
        offsetYDp = lengths[1].coerceIn(-MAX_SHADOW_DP, MAX_SHADOW_DP),
        blurDp = lengths.getOrElse(2) { 0f }.coerceIn(0f, MAX_SHADOW_DP),
        spreadDp = lengths.getOrElse(3) { 0f }.coerceIn(-MAX_SHADOW_DP, MAX_SHADOW_DP),
        colorArgb = color,
        inset = inset,
    )
}

private const val MAX_SHADOW_DP = 64f
