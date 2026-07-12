package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.supportsEntryType
import logcat.LogPriority
import mihon.entry.interactions.EntryConsumptionInteraction
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.EntryTrack
import kotlin.math.max

class SyncChapterProgressWithTrack(
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val entryConsumptionInteraction: EntryConsumptionInteraction,
    private val insertTrack: InsertTrack,
) {

    suspend fun await(
        entryId: Long,
        remoteTrack: EntryTrack,
        tracker: Tracker,
    ) {
        if (tracker !is EnhancedTracker) {
            return
        }
        val entry = entryRepository.getEntryById(entryId) ?: return
        if (!tracker.supportsEntryType(entry.type)) {
            return
        }

        val sortedChapters = entryChapterRepository.getChaptersByEntryIdAwait(entryId)
            .sortedBy { it.chapterNumber }
            .filter { it.isRecognizedNumber }

        val chaptersToUpdate = sortedChapters
            .filter { chapter -> chapter.chapterNumber <= remoteTrack.progress && !chapter.read }

        // only take into account continuous reading
        val localLastRead = sortedChapters.takeWhile { it.read }.lastOrNull()?.chapterNumber ?: 0F
        val lastRead = max(remoteTrack.progress, localLastRead.toDouble())
        val updatedTrack = remoteTrack.copy(progress = lastRead)

        try {
            tracker.update(updatedTrack.toDbTrack())
            if (chaptersToUpdate.isNotEmpty()) {
                entryConsumptionInteraction.setConsumed(entry, chaptersToUpdate, consumed = true)
            }
            insertTrack.await(updatedTrack)
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e)
        }
    }
}
