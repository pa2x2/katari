package app.katari.entry.interactions.media.backup

import app.katari.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupStateParticipant
import app.katari.entry.interactions.persistence.backup.decodeEntryBackupState
import app.katari.entry.interactions.persistence.backup.entryBackupStateEnvelope
import kotlinx.serialization.Serializable
import tachiyomi.domain.entry.model.TheIntroDbMatch
import tachiyomi.domain.entry.model.TheIntroDbMedia
import tachiyomi.domain.entry.model.TheIntroDbPlacement
import tachiyomi.domain.entry.repository.TheIntroDbMatchRepository

internal const val ENTRY_THEINTRODB_MATCHES_BACKUP_STATE_ID = "entry.theintrodb-matches.backup"
internal const val ENTRY_THEINTRODB_MATCHES_BACKUP_SCHEMA_VERSION = 1

/**
 * The series or film the viewer picked for an entry's TheIntroDB times, None included. Matches found by title are left
 * out, as for AniSkip's: a restored entry finds them again the first time it plays.
 */
internal class EntryTheIntroDbMatchesBackupParticipant(
    private val repository: () -> TheIntroDbMatchRepository,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_THEINTRODB_MATCHES_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        event.entries.forEach { entry ->
            val picked = repository().getPickedByEntryId(entry.id)
            if (picked.isEmpty()) return@forEach
            event.contributions.add(
                entry,
                entryBackupStateEnvelope(
                    stateId,
                    ENTRY_THEINTRODB_MATCHES_BACKUP_SCHEMA_VERSION,
                    TheIntroDbMatchesBackupState.serializer(),
                    TheIntroDbMatchesBackupState(picked.map { it.toBackup() }),
                ),
            )
        }
    }

    override suspend fun restore(event: EntryBackupRestoreEvent) {
        val state = event.states.decodeEntryBackupState(
            stateId,
            ENTRY_THEINTRODB_MATCHES_BACKUP_SCHEMA_VERSION,
            TheIntroDbMatchesBackupState.serializer(),
        ) ?: return
        state.matches.forEach { repository().upsert(it.toMatch(event.entry.id)) }
    }
}

@Serializable
private data class TheIntroDbMatchesBackupState(val matches: List<TheIntroDbMatchBackup>)

/** A match without [title] is None; [season] and [firstEpisode] are null for a film. */
@Serializable
private data class TheIntroDbMatchBackup(
    val groupKey: String? = null,
    val tvdbId: Long? = null,
    val tmdbId: Long? = null,
    val imdbId: String? = null,
    val title: String? = null,
    val year: Int? = null,
    val season: Int? = null,
    val firstEpisode: Int? = null,
    val pickedAt: Long,
)

private fun TheIntroDbMatch.toBackup() = TheIntroDbMatchBackup(
    groupKey = groupKey,
    tvdbId = media?.tvdbId,
    tmdbId = media?.tmdbId,
    imdbId = media?.imdbId,
    title = media?.title,
    year = media?.year,
    season = media?.placement?.season,
    firstEpisode = media?.placement?.firstEpisode,
    pickedAt = checkedAt,
)

private fun TheIntroDbMatchBackup.toMatch(entryId: Long): TheIntroDbMatch {
    val placement = if (season != null && firstEpisode != null) TheIntroDbPlacement(season, firstEpisode) else null
    return TheIntroDbMatch(
        entryId = entryId,
        groupKey = groupKey,
        media = title?.let { TheIntroDbMedia(tvdbId, tmdbId, imdbId, it, year, placement) },
        pickedByUser = true,
        checkedAt = pickedAt,
    )
}
