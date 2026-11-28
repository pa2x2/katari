package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import app.katari.entry.interactions.reader.settings.BookDocumentReaderTheme
import kotlin.math.roundToInt

/**
 * LNReader's reader colours as Katari's custom theme, or null where one isn't a hex colour. Katari's colours are
 * opaque, so a text colour LNReader dims with alpha, as its black preset's `#FFFFFFB3`, is blended over the background
 * to keep its look (D082); a background's alpha has nothing to blend over and is dropped.
 */
internal fun LnReaderReaderColors.toKatariTheme(): BookDocumentReaderTheme.Custom? {
    val background = cssHexColor(backgroundColor)?.let { it or OPAQUE_BLACK } ?: return null
    val text = cssHexColor(textColor) ?: return null
    return BookDocumentReaderTheme.Custom(background, text.blendedOver(background))
}

/** An ARGB colour from CSS's `#RGB`, `#RGBA`, `#RRGGBB` or `#RRGGBBAA`, whose alpha comes last. */
private fun cssHexColor(css: String): Int? {
    val hex = HEX_COLOR.matchEntire(css.trim())?.groupValues?.get(1) ?: return null
    val full = if (hex.length <= 4) hex.map { "$it$it" }.joinToString("") else hex
    val rgb = full.substring(0, 6).toInt(16)
    val alpha = if (full.length == 8) full.substring(6).toInt(16) else 0xFF
    return (alpha shl 24) or rgb
}

/** This colour as it shows over the opaque [background]. */
private fun Int.blendedOver(background: Int): Int {
    val alpha = (this ushr 24) / 255.0
    fun channel(shift: Int): Int {
        val over = (this shr shift) and 0xFF
        val under = (background shr shift) and 0xFF
        return (over * alpha + under * (1 - alpha)).roundToInt()
    }
    return OPAQUE_BLACK or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

private val HEX_COLOR = Regex("#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})")

private const val OPAQUE_BLACK = 0xFF000000.toInt()
