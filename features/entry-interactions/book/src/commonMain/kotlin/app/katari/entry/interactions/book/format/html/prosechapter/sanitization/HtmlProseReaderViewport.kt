package app.katari.entry.interactions.book.format.html.prosechapter.sanitization

import app.katari.entry.interactions.book.format.html.prosechapter.css.CssViewport

/**
 * The page a chapter's `@media` queries see: the screen in density-independent pixels, which a phone browser
 * reports as CSS pixels, when the chapter is prepared. A site's narrow-screen rules then apply on phones and its
 * wide ones on tablets and desktops.
 */
internal fun currentReaderViewport(): CssViewport =
    screenViewport()?.takeIf { it.widthPx > 0f && it.heightPx > 0f } ?: PHONE_READER_VIEWPORT

/** The screen in density-independent pixels, or `null` when no screen is known. */
internal expect fun screenViewport(): CssViewport?

// A phone in portrait, for when no screen is known, such as in tests.
internal val PHONE_READER_VIEWPORT = CssViewport(widthPx = 412f, heightPx = 915f)
