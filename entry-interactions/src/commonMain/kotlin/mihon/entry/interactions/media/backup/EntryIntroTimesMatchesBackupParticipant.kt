package mihon.entry.interactions.media.backup

import kotlinx.serialization.Serializable
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope
import tachiyomi.domain.entry.model.IntroTimesMatch
import tachiyomi.domain.entry.model.IntroTimesSeries
import tachiyomi.domain.entry.repository.IntroTimesMatchRepository

internal const val ENTRY_INTRO_TIMES_MATCHES_BACKUP_STATE_ID = "entry.intro-times-matches.backup"
internal const val ENTRY_INTRO_TIMES_MATCHES_BACKUP_SCHEMA_VERSION = 1

/**
 * The series the viewer picked for an entry's intro and credits times, None included. Matches found by title are left
 * out: a restored entry finds them again the first time it plays.
 */
internal class EntryIntroTimesMatchesBackupParticipant(
    private val repository: () -> IntroTimesMatchRepository,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_INTRO_TIMES_MATCHES_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        event.entries.forEach { entry ->
            val picked = repository().getPickedByEntryId(entry.id)
            if (picked.isEmpty()) return@forEach
            event.contributions.add(
                entry,
                entryBackupStateEnvelope(
                    stateId,
                    ENTRY_INTRO_TIMES_MATCHES_BACKUP_SCHEMA_VERSION,
                    IntroTimesMatchesBackupState.serializer(),
                    IntroTimesMatchesBackupState(picked.map { it.toBackup() }),
                ),
            )
        }
    }

    override suspend fun restore(event: EntryBackupRestoreEvent) {
        val state = event.states.decodeEntryBackupState(
            stateId,
            ENTRY_INTRO_TIMES_MATCHES_BACKUP_SCHEMA_VERSION,
            IntroTimesMatchesBackupState.serializer(),
        ) ?: return
        state.matches.forEach { repository().upsert(it.toMatch(event.entry.id)) }
    }
}

@Serializable
private data class IntroTimesMatchesBackupState(val matches: List<IntroTimesMatchBackup>)

/** A match without [anilistId] is None. */
@Serializable
private data class IntroTimesMatchBackup(
    val groupKey: String? = null,
    val anilistId: Long? = null,
    val malId: Long? = null,
    val title: String? = null,
    val year: Int? = null,
    val pickedAt: Long,
)

private fun IntroTimesMatch.toBackup() = IntroTimesMatchBackup(
    groupKey = groupKey,
    anilistId = series?.anilistId,
    malId = series?.malId,
    title = series?.title,
    year = series?.year,
    pickedAt = checkedAt,
)

private fun IntroTimesMatchBackup.toMatch(entryId: Long): IntroTimesMatch {
    val series = if (anilistId != null && malId != null && title != null) {
        IntroTimesSeries(anilistId = anilistId, malId = malId, title = title, year = year)
    } else {
        null
    }
    return IntroTimesMatch(
        entryId = entryId,
        groupKey = groupKey,
        series = series,
        pickedByUser = true,
        checkedAt = pickedAt,
    )
}
