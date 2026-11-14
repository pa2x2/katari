package app.katari.sync.client.kind.libraryupdate

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.merge.latest
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind
import tachiyomi.data.library.update.entryUpdateStatus
import tachiyomi.data.library.update.libraryUpdateRun
import tachiyomi.domain.library.update.model.LibraryUpdateRunSummary

/**
 * The newest update of a profile's whole library, keyed by profile uuid, so every device can say when the library was
 * last checked and how that went, wherever it ran. Local ref: `[profile_id]`, the profile's row of
 * `library_update_runs`.
 *
 * Each device keeps its own update's report in its rows; another device's update stays in the record, which
 * [app.katari.sync.client.libraryupdate.OtherDeviceLibraryUpdates] reads. The record is captured as the update starts
 * and finishes, so it doesn't follow the entries a later update of part of the library rechecks.
 */
object LibraryUpdateKind : SyncedKind<LibraryUpdatePayload>(
    "library.update",
    1,
    LibraryUpdatePayload.serializer(),
    Absence.KEEP,
) {
    private const val LATEST = 2

    override fun profileOf(payload: LibraryUpdatePayload) = payload.profileUuid

    override val capture = listOf(
        CaptureTrigger("library_update_runs", Event.INSERT, listOf("NEW.profile_id")),
        CaptureTrigger(
            "library_update_runs",
            Event.UPDATE,
            listOf("NEW.profile_id"),
            fields = listOf(Field(LATEST, "started_at", "finished_at", "trigger", "library_size")),
        ),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<LibraryUpdatePayload> {
        val profileId = ref.long(0)
        val run = reads.database.library_update_runsQueries.getByProfile(profileId, ::libraryUpdateRun)
            .awaitAsOneOrNull()
            ?: return LocalRead.Missing
        if (!run.coversLibrary) return LocalRead.NotSynced
        val profileUuid = reads.profileUuid(profileId) ?: return LocalRead.Missing
        val statuses = reads.database.entry_update_statusQueries
            .getDecidedSince(profileId, run.startedAt, ::entryUpdateStatus)
            .awaitAsList()
        val update = SyncedLibraryUpdate.of(reads.device, LibraryUpdateRunSummary.of(run, statuses))
        return LocalRead.Found(
            key = RecordKeys.derive(name, profileUuid),
            parents = listOf(RecordRef(ProfileKind, ProfileKind.ref(profileUuid))),
        ) { previous, stamps ->
            val local = stamps.field(LATEST, update, previous?.latest)
            LibraryUpdatePayload(
                profileUuid = profileUuid,
                // A newer update another device made stays when this device's older one is read again
                latest = previous?.let { newer(it.latest, local) } ?: local,
            )
        }
    }

    override fun merge(local: LibraryUpdatePayload, incoming: LibraryUpdatePayload) =
        local.copy(latest = newer(local.latest, incoming.latest))

    // The later start wins; the same update's later edit, such as its finish, replaces its start
    private fun newer(a: Stamped<SyncedLibraryUpdate>, b: Stamped<SyncedLibraryUpdate>) = when {
        a.value.startedAt > b.value.startedAt -> a
        b.value.startedAt > a.value.startedAt -> b
        else -> latest(a, b)
    }

    // Nothing is written: this device's update is its own row, and another device's has no rows here to go to
    override suspend fun apply(writes: LocalWrites, key: String, payload: LibraryUpdatePayload): ApplyOutcome {
        val profileId = writes.profileId(payload.profileUuid).idOr { return it }
        return ApplyOutcome.Applied(LocalRef.of(profileId))
    }
}
