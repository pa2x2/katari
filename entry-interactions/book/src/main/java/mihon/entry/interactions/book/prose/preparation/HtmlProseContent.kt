package mihon.entry.interactions.book.prose

import mihon.book.api.BookContentResource

internal fun BookContentResource.isHtmlResource(): Boolean = when (
    mediaType?.substringBefore(';')?.trim()?.lowercase()
) {
    null, HTML_MEDIA_TYPE, XHTML_MEDIA_TYPE -> true
    else -> false
}

internal const val HTML_MEDIA_TYPE = "text/html"
internal const val PROSE_CHAPTER_PROFILE = "prose-chapter"
internal const val XHTML_MEDIA_TYPE = "application/xhtml+xml"
