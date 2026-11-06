package app.katari.sync.client.kind.replacement

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.chapter.ChapterKind
import app.katari.sync.client.record.LocalReads

/**
 * What an entry moved to another profile became. A move takes the entry's chapters and activity along, so activity
 * recorded on the old entry by a device that hadn't heard of the move lands on the new one, as moving it here would
 * have left it.
 */
internal suspend fun LocalWrites.entryFollowingMove(entryKey: String): String = movedTo(entryKey) ?: entryKey

/** The key of the chapter [chapterKey] under the entry its entry moved to, which keeps the chapter's URL. */
internal suspend fun LocalWrites.chapterFollowingMove(chapterKey: String): String {
    val stored = database.sync_recordsQueries.getByKey(chapterKey).awaitAsOneOrNull()?.takeUnless { it.deleted }
        ?: return chapterKey
    val chapter = ChapterKind.decode(stored.payload)
    val to = movedTo(chapter.entryKey) ?: return chapterKey
    return RecordKeys.derive(LocalReads.CHAPTER, to, chapter.url)
}
