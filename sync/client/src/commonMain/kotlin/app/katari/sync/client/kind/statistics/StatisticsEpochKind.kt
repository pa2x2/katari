package app.katari.sync.client.kind.statistics

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
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind

/** Statistics epochs, keyed by profile uuid. Local ref: `[profile_id]`. */
object StatisticsEpochKind : SyncedKind<StatisticsEpochPayload>(
    "statistics.epoch",
    1,
    StatisticsEpochPayload.serializer(),
    Absence.KEEP,
) {
    override val capture = listOf(
        CaptureTrigger("statistics_epochs", Event.INSERT, listOf("NEW.profile_id")),
        CaptureTrigger(
            "statistics_epochs",
            Event.UPDATE,
            listOf("NEW.profile_id"),
            fields = listOf(Field(2, "started_at")),
        ),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<StatisticsEpochPayload> {
        val row = reads.database.sync_local_rowsQueries.statisticsEpochRow(ref.long(0)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val profileUuid = reads.profileUuid(row.profile_id) ?: return LocalRead.Missing
        return LocalRead.Found(
            key = RecordKeys.derive(name, profileUuid),
            parents = listOf(RecordRef(ProfileKind, ProfileKind.ref(profileUuid))),
        ) { previous, stamps ->
            StatisticsEpochPayload(profileUuid, stamps.field(2, row.started_at, previous?.startedAt))
        }
    }
    override fun merge(local: StatisticsEpochPayload, incoming: StatisticsEpochPayload) =
        local.copy(startedAt = latest(local.startedAt, incoming.startedAt))

    override suspend fun apply(writes: LocalWrites, key: String, payload: StatisticsEpochPayload): ApplyOutcome {
        val profileId = writes.profileId(payload.profileUuid).idOr { return it }
        writes.database.sync_applyQueries.upsertStatisticsEpoch(profileId, payload.startedAt.value)
        return ApplyOutcome.Applied(LocalRef.of(profileId))
    }
}
