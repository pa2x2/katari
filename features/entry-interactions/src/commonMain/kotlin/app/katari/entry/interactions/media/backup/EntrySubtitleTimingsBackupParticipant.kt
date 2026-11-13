package app.katari.entry.interactions.media.backup

import app.katari.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupStateParticipant
import app.katari.entry.interactions.persistence.backup.decodeEntryBackupState
import app.katari.entry.interactions.persistence.backup.entryBackupStateEnvelope
import kotlinx.serialization.Serializable
import tachiyomi.domain.entry.model.SubtitleTiming
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.SubtitleTimingRepository

internal const val ENTRY_SUBTITLE_TIMINGS_BACKUP_STATE_ID = "entry.subtitle-timings.backup"
internal const val ENTRY_SUBTITLE_TIMINGS_BACKUP_SCHEMA_VERSION = 1

/**
 * The subtitle timings set on an entry's episodes, each under its episode's URL, so a restore finds the episode again.
 * They go with the episodes: a backup without chapters leaves them out, and a restored timing whose episode isn't there
 * is dropped.
 */
internal class EntrySubtitleTimingsBackupParticipant(
    private val timings: () -> SubtitleTimingRepository,
    private val chapters: () -> EntryChapterRepository,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_SUBTITLE_TIMINGS_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        if (!event.selection.includeContentState) return
        event.entries.forEach { entry ->
            val entryTimings = timings().getByEntryId(entry.id)
            if (entryTimings.isEmpty()) return@forEach
            val urls = chapters().getChaptersByEntryIdAwait(entry.id).associate { it.id to it.url }
            val backups = entryTimings.mapNotNull { timing ->
                urls[timing.chapterId]?.let { SubtitleTimingBackup(it, timing.subtitleKey, timing.delayMs) }
            }
            event.contributions.add(
                entry,
                entryBackupStateEnvelope(
                    stateId,
                    ENTRY_SUBTITLE_TIMINGS_BACKUP_SCHEMA_VERSION,
                    SubtitleTimingsBackupState.serializer(),
                    SubtitleTimingsBackupState(backups),
                ),
            )
        }
    }

    override suspend fun restore(event: EntryBackupRestoreEvent) {
        val state = event.states.decodeEntryBackupState(
            stateId,
            ENTRY_SUBTITLE_TIMINGS_BACKUP_SCHEMA_VERSION,
            SubtitleTimingsBackupState.serializer(),
        ) ?: return
        state.timings.forEach { backup ->
            if (backup.delayMs == 0L) return@forEach
            val chapter = chapters().getChapterByUrlAndEntryId(backup.chapterUrl, event.entry.id) ?: return@forEach
            timings().set(SubtitleTiming(chapter.id, backup.subtitleKey, backup.delayMs))
        }
    }
}

@Serializable
private data class SubtitleTimingsBackupState(val timings: List<SubtitleTimingBackup>)

@Serializable
private data class SubtitleTimingBackup(
    val chapterUrl: String,
    val subtitleKey: String,
    val delayMs: Long,
)
