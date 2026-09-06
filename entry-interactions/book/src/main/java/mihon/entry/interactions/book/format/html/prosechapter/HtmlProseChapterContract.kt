package mihon.entry.interactions.book.format.html.prosechapter

import mihon.book.api.BookContentDescriptor

internal object HtmlProseChapterContract {
    const val FORMAT = "text/html"
    const val PROFILE = "prose-chapter"
    const val PROTECTION = "none"

    const val MAX_RAW_BYTES = 4 * 1024 * 1024
    const val MAX_CANONICAL_UTF16 = 2_000_000
    const val MAX_DOM_NODES = 100_000
    const val MAX_BLOCKS = 20_000
    const val MAX_DOM_DEPTH = 32
    const val MAX_LIST_DEPTH = 8
    const val MAX_CSS_RULES = 256
    const val MAX_CSS_DECLARATIONS_PER_RULE = 64
    const val MAX_TABLE_COLUMNS = 24

    val descriptor = BookContentDescriptor(
        format = FORMAT,
        profile = PROFILE,
        protection = PROTECTION,
    )

    fun supports(candidate: BookContentDescriptor): Boolean = candidate == descriptor
}

internal class HtmlProseLimitExceededException(message: String) : IllegalArgumentException(message)
