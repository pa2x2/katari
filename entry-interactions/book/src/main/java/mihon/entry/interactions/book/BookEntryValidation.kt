package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry

internal fun Entry.requireBook() {
    require(type == EntryType.BOOK) {
        "Book entry interaction received ${type.name}; expected ${EntryType.BOOK.name}"
    }
}
