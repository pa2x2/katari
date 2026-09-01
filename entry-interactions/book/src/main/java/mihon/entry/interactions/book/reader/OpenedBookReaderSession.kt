package mihon.entry.interactions.book.reader

import mihon.book.api.BookLocator
import mihon.entry.interactions.book.content.BookContentSession
import mihon.entry.interactions.book.preparation.PreparedBookPublication
import mihon.entry.interactions.book.state.BookProgressIdentity
import mihon.entry.interactions.book.state.BookProgressLocatorCodec
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.session.EntryMediaSessionActivitySession
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.viewer.settings.shared.ReaderCapabilityId
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState

internal class OpenedBookReaderSession(
    val entry: Entry,
    val owner: Entry,
    val chapter: EntryChapter,
    private val progressIdentity: BookProgressIdentity,
    contentSession: BookContentSession,
    val preparedPublication: PreparedBookPublication,
    val initialLocator: BookLocator?,
    private val mediaSession: EntryMediaSessionProcessor,
    private val now: () -> Long,
    val readerSettingsSurfaceId: String? = null,
    val readerCapabilities: Set<ReaderCapabilityId> = emptySet(),
) : AutoCloseable {
    private val defaultActivitySession = EntryMediaSessionActivitySession()
    private val closeStack = BookSessionCloseStack().apply {
        own(contentSession)
        own(preparedPublication)
    }

    suspend fun saveLocation(locator: BookLocator, completed: Boolean = false) {
        val timestamp = now()
        val shouldBeCompleted = chapter.read || completed
        val progress = EntryProgressState(
            entryId = chapter.entryId,
            chapterId = chapter.id,
            contentKey = progressIdentity.contentKey,
            resourceKey = progressIdentity.resourceKey,
            resourceRevision = progressIdentity.resourceRevision,
            locator = BookProgressLocatorCodec.encode(locator),
            locatorUpdatedAt = timestamp,
            completed = shouldBeCompleted,
            completionUpdatedAt = if (shouldBeCompleted) timestamp else 0L,
        )
        mediaSession.onEvent(
            EntryMediaSessionEvent.Progressed(
                visibleEntry = entry,
                child = chapter,
                progress = progress,
                fraction = locator.totalProgression ?: locator.progression,
                preserveLocatorExtensions = true,
            ),
        )
    }

    suspend fun recordHistory(
        sessionReadDuration: Long,
        activitySession: EntryMediaSessionActivitySession = defaultActivitySession,
    ) {
        if (sessionReadDuration <= 0L) return
        mediaSession.onEvent(
            EntryMediaSessionEvent.ActivityRecorded(
                visibleEntry = entry,
                child = chapter,
                activity = activitySession.record(
                    recordedAtEpochMillis = now(),
                    durationMillis = sessionReadDuration,
                ),
            ),
        )
    }

    override fun close() = closeStack.close()
}
