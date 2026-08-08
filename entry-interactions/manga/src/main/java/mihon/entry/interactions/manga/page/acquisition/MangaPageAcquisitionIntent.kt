package mihon.entry.interactions.manga.page.acquisition

/**
 * Explains why a page is being acquired so shared acquisition can reserve
 * progressive decoding for content a user can currently see.
 */
internal enum class MangaPageAcquisitionIntent {
    Visible,
    Preload,
}
