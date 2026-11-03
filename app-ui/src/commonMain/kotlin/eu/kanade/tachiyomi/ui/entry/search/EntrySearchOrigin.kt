package eu.kanade.tachiyomi.ui.entry.search

/**
 * A screen an entry was opened from that searches for what the entry screen asks, such as a tag or its source's name,
 * once the entry screen closes: the home screen searches the library, a catalogue its source.
 */
interface EntrySearchOrigin {
    suspend fun search(query: String)
}

/** An [EntrySearchOrigin] that also searches a genre, as a source's catalogue does through its filters. */
interface EntryGenreSearchOrigin : EntrySearchOrigin {
    suspend fun searchGenre(name: String)
}
