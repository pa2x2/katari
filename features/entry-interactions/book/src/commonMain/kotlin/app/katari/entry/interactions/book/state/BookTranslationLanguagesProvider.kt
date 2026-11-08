package app.katari.entry.interactions.book.state

import app.katari.entry.interactions.state.EntryTranslationLanguagesProvider

/** Book text is translated from the reader, so each book keeps its own content and target languages. */
internal object BookTranslationLanguagesProvider : EntryTranslationLanguagesProvider
