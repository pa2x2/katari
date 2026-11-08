package app.katari.entry.interactions.manga.reader.text.image

/**
 * Opens [still] for text recognition, or returns null for content the decoder can't read or a platform without text
 * recognition.
 */
internal expect suspend fun openDisplayedPageImage(still: DisplayedStillImage): DisplayedPageImage?
