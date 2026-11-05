package mihon.entry.interactions.manga.translation.context

import mihon.translation.api.request.TranslationContext
import mihon.translation.api.request.TranslationWorkContext

/** What a series says about its pages, for translating them; null without a title. */
internal fun mangaWorkContext(title: String?, description: String?): TranslationWorkContext? {
    if (title.isNullOrBlank()) return null
    return TranslationWorkContext(
        title = title.trim(),
        description = description?.trim()?.takeCodePoints(MAXIMUM_DESCRIPTION_CODE_POINTS)?.ifBlank { null },
    )
}

/**
 * The context the text of a page is translated in: its series, and the end of [precedingText], which is the text
 * read right before it in the same chapter. Only the end is kept, since what a speech bubble continues from or
 * refers to is rarely further back, and everything sent costs time with every page.
 */
internal fun mangaPageContext(work: TranslationWorkContext?, precedingText: List<String>): TranslationContext {
    var codePoints = 0
    val end = precedingText.takeLastWhile { text ->
        codePoints += text.codePointCount(0, text.length)
        codePoints <= MAXIMUM_PRECEDING_CODE_POINTS
    }
    return TranslationContext(work = work, precedingText = end)
}

private fun String.takeCodePoints(count: Int): String =
    if (codePointCount(0, length) <= count) this else substring(0, offsetByCodePoints(0, count))

private const val MAXIMUM_DESCRIPTION_CODE_POINTS = 500
private const val MAXIMUM_PRECEDING_CODE_POINTS = 600
