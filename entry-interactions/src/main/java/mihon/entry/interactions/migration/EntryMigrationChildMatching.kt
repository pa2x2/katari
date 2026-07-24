package mihon.entry.interactions

import mihon.entry.interactions.host.EntryMigrationHostChildUpdate
import tachiyomi.domain.entry.model.EntryChapter

internal fun prepareMigrationChildUpdates(
    sourceChildren: List<EntryChapter>,
    targetChildren: List<EntryChapter>,
    transferConsumption: Boolean,
    transferBookmarks: Boolean,
): List<EntryMigrationHostChildUpdate> {
    val maxConsumedNumber = sourceChildren
        .filter(EntryChapter::read)
        .mapNotNull { child -> child.chapterNumber.takeIf { it >= 0.0 } }
        .maxOrNull()

    return targetChildren.mapNotNull { target ->
        val source = findMigrationSourceChild(target, sourceChildren)
        var read = target.read
        var bookmark = target.bookmark
        var dateFetch = target.dateFetch
        if (source != null) {
            if (transferConsumption) read = source.read
            if (transferBookmarks) bookmark = source.bookmark
            dateFetch = source.dateFetch
        }
        if (
            transferConsumption &&
            maxConsumedNumber != null &&
            target.chapterNumber >= 0.0 &&
            target.chapterNumber <= maxConsumedNumber
        ) {
            read = true
        }
        val updated = target.copy(read = read, bookmark = bookmark, dateFetch = dateFetch)
        updated.takeIf { it != target }?.let { EntryMigrationHostChildUpdate(target, it) }
    }
}

internal fun findMigrationSourceChild(
    target: EntryChapter,
    sourceChildren: List<EntryChapter>,
): EntryChapter? {
    return if (target.chapterNumber >= 0.0) {
        sourceChildren.firstOrNull { source ->
            source.chapterNumber >= 0.0 && source.chapterNumber == target.chapterNumber
        }
    } else {
        sourceChildren.firstOrNull { source -> source.name == target.name }
    }
}
