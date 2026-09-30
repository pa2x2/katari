package mihon.entry.interactions.manga.reader.text.stored

import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.toEntryChapter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslation
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslationStore
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

/** Stored translations of the chapters a reader opens, read once per chapter. */
internal class MangaReaderStoredTranslations(
    private val store: MangaChapterTranslationStore,
    private val downloadManager: DownloadManager,
    private val sourceManager: SourceManager,
    private val series: () -> Entry?,
) {
    private val mutex = Mutex()
    private val read = mutableMapOf<Long, MangaChapterTranslation?>()

    /** The stored translation of [chapter], or `null` when it is not downloaded or has none. */
    suspend fun of(chapter: ReaderChapter): MangaChapterTranslation? {
        val id = chapter.chapter.id ?: return null
        return mutex.withLock {
            if (id in read) return@withLock read[id]
            load(chapter).also { read[id] = it }
        }
    }

    /** Deletes the stored translation of [chapter] and keeps its download. */
    suspend fun delete(chapter: ReaderChapter) {
        val id = chapter.chapter.id ?: return
        mutex.withLock {
            located(chapter)?.let { (entryChapter, entry, source) -> store.delete(entryChapter, entry, source) }
            read[id] = null
        }
    }

    private suspend fun load(chapter: ReaderChapter): MangaChapterTranslation? {
        val (entryChapter, entry, source) = located(chapter) ?: return null
        val downloaded = downloadManager.isChapterDownloaded(
            entryChapter.name,
            entryChapter.scanlator,
            entryChapter.url,
            entry.title,
            entry.source,
        )
        return if (downloaded) store.read(entryChapter, entry, source) else null
    }

    private fun located(chapter: ReaderChapter): Located? {
        val entryChapter = chapter.chapter.toDomainChapter()?.toEntryChapter() ?: return null
        val entry = chapter.manga ?: series() ?: return null
        val source = sourceManager.get(entry.source) ?: return null
        return Located(entryChapter, entry, source)
    }

    private data class Located(val chapter: EntryChapter, val entry: Entry, val source: UnifiedSource)
}
