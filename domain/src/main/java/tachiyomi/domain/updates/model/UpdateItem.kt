package tachiyomi.domain.updates.model

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.library.model.LibraryItemKey

sealed class UpdateItem {
    abstract val key: LibraryItemKey
    abstract val entryId: Long
    abstract val entryType: EntryType
    abstract val entryTitle: String
    abstract val coverData: EntryCover
    abstract val sourceId: Long
    abstract val dateFetch: Long
    abstract val consumed: Boolean
    abstract val started: Boolean

    data class EntryUpdate(val update: UpdatesWithRelations, val entryTypeArg: EntryType) : UpdateItem() {
        override val key = LibraryItemKey(entryTypeArg, update.chapterId)
        override val entryId = update.entryId
        override val entryType = entryTypeArg
        override val entryTitle = update.entryTitle
        override val coverData = update.coverData
        override val sourceId = update.sourceId
        override val dateFetch = update.dateFetch
        override val consumed = update.read
        override val started = update.started
    }
}

fun UpdatesWithRelations.toUpdateItem(type: EntryType): UpdateItem {
    return UpdateItem.EntryUpdate(this, type)
}
