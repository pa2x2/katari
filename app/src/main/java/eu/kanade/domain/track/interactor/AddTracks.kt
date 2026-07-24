package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.util.lang.convertEpochMillisZone
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.EntryTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZoneOffset

class AddTracks(
    private val insertTrack: InsertTrack,
    private val entryChapterRepository: EntryChapterRepository,
) {

    // TODO: update all trackers based on common data
    suspend fun bind(tracker: Tracker, item: Track, entryId: Long) = withNonCancellableContext {
        withIOContext {
            val allChapters = entryChapterRepository.getChaptersByEntryIdAwait(entryId)
            val hasReadChapters = allChapters.any { it.read }
            tracker.bind(item, hasReadChapters)

            var track = item.toDomainTrack(idRequired = false) ?: return@withIOContext

            insertTrack.await(track)

            // TODO: merge into [SyncChapterProgressWithTrack]?
            // Update chapter progress if newer chapters marked read locally
            if (hasReadChapters) {
                val latestLocalReadChapterNumber = allChapters
                    .sortedBy { it.chapterNumber }
                    .takeWhile { it.read }
                    .lastOrNull()
                    ?.chapterNumber ?: -1.0

                if (latestLocalReadChapterNumber > track.progress) {
                    track = track.copy(
                        progress = latestLocalReadChapterNumber,
                    )
                    tracker.setRemoteLastChapterRead(track.toDbTrack(), latestLocalReadChapterNumber.toInt())
                }

                if (track.startDate <= 0) {
                    val firstReadChapterDate = Injekt.get<GetHistory>().await(entryId)
                        .sortedBy { it.readAt }
                        .firstOrNull()
                        ?.readAt

                    firstReadChapterDate?.let {
                        val startDate = firstReadChapterDate.time.convertEpochMillisZone(
                            ZoneOffset.systemDefault(),
                            ZoneOffset.UTC,
                        )
                        track = track.copy(
                            startDate = startDate,
                        )
                        tracker.setRemoteStartDate(track.toDbTrack(), startDate)
                    }
                }
            }
        }
    }

    suspend fun bindEnhancedTracker(entry: Entry, tracker: Tracker): EntryTrack? = withNonCancellableContext {
        withIOContext {
            val enhancedTracker = tracker as? EnhancedTracker ?: return@withIOContext null
            val candidate = enhancedTracker.match(entry) ?: return@withIOContext null
            candidate.manga_id = entry.id
            tracker.bind(candidate)
            val track = candidate.toDomainTrack(idRequired = false) ?: return@withIOContext null
            insertTrack.await(entry.profileId, track)
            track
        }
    }
}
