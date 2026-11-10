package app.katari.entry.interactions.manga.reader.image.background

/**
 * A stretch of the space beside a page from [top] to [bottom], as fractions of the page's height, running from
 * [topArgb] to [bottomArgb]; the two are the same where it holds one colour. ARGB without premultiplied alpha.
 */
internal data class BackdropSpan(val top: Float, val bottom: Float, val topArgb: Int, val bottomArgb: Int)
