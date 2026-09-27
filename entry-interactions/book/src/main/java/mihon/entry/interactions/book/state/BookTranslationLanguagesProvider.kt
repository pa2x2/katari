package mihon.entry.interactions.book.state

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.state.EntryTranslationLanguagesProvider

/** Book text is translated from the reader, so each book keeps its own content and target languages. */
internal object BookTranslationLanguagesProvider : EntryTranslationLanguagesProvider {
    override val type: EntryType = EntryType.BOOK
}
