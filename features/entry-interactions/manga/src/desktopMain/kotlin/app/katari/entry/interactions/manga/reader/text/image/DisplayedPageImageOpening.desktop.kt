package app.katari.entry.interactions.manga.reader.text.image

// No recognition engine runs on the desktop, so nothing reads page text there
internal actual suspend fun openDisplayedPageImage(still: DisplayedStillImage): DisplayedPageImage? = null
