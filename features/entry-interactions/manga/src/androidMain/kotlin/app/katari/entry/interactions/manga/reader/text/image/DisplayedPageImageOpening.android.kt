package app.katari.entry.interactions.manga.reader.text.image

internal actual suspend fun openDisplayedPageImage(still: DisplayedStillImage): DisplayedPageImage? =
    MangaDisplayedPageImage.open(still)
