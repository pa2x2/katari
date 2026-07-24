package eu.kanade.domain.track.interactor

import android.content.Context
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.domain.track.service.DelayedTrackingUpdateJob
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack

class TrackChapter(
    private val getTracks: GetTracks,
    private val trackerManager: TrackerManager,
    private val insertTrack: InsertTrack,
    private val delayedTrackingStore: DelayedTrackingStore,
) {

    suspend fun await(
        context: Context,
        entryId: Long,
        chapterNumber: Double,
        serviceIds: Set<Long>,
        setupJobOnFailure: Boolean = true,
    ): List<TrackChapterFailure> {
        return withNonCancellableContext {
            val tracks = getTracks.await(entryId)
            if (tracks.isEmpty()) return@withNonCancellableContext emptyList()

            tracks.mapNotNull { track ->
                val service = trackerManager.get(track.trackerId)
                if (service == null || service.id !in serviceIds || !service.isLoggedIn ||
                    chapterNumber <= track.progress
                ) {
                    return@mapNotNull null
                }

                async {
                    service to runCatching {
                        try {
                            val updatedTrack = service.refresh(track.toDbTrack())
                                .toDomainTrack(idRequired = true)!!
                                .copy(progress = chapterNumber)
                            service.update(updatedTrack.toDbTrack(), true)
                            insertTrack.await(updatedTrack)
                            delayedTrackingStore.remove(track.id)
                        } catch (e: Exception) {
                            delayedTrackingStore.add(track.id, chapterNumber)
                            if (setupJobOnFailure) {
                                DelayedTrackingUpdateJob.setupTask(context)
                            }
                            throw e
                        }
                    }
                }
            }
                .awaitAll()
                .mapNotNull { (service, result) ->
                    result.exceptionOrNull()?.let { error ->
                        logcat(LogPriority.WARN, error)
                        TrackChapterFailure(service.id, service.name, error)
                    }
                }
        }
    }
}

data class TrackChapterFailure(
    val serviceId: Long,
    val serviceName: String,
    val cause: Throwable,
)
