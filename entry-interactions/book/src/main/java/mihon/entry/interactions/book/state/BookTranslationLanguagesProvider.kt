package mihon.entry.interactions.book.state

import mihon.entry.interactions.state.EntryTranslationLanguagesProvider

/** Book text is translated from the reader, so each book keeps its own content and target languages. */
internal object BookTranslationLanguagesProvider : EntryTranslationLanguagesProvider
