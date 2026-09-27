package mihon.entry.interactions.manga.state

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.state.EntryTranslationLanguagesProvider

/** Manga pages are recognized and translated, so each series keeps its own page and target languages. */
internal object MangaTranslationLanguagesProvider : EntryTranslationLanguagesProvider {
    override val type: EntryType = EntryType.MANGA
}
