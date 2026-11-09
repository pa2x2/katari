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
import app.katari.sync.client.merge.earlier
import app.katari.sync.client.merge.latest
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind

/**
 * Statistics epochs, keyed by profile uuid. Local ref: `[profile_id]`.
 *
 * Version 1 merged starts to the later one, which hid what the device that started counting first had recorded. Its
 * records read as starts with no clear.
 */
object StatisticsEpochKind : SyncedKind<StatisticsEpochPayload>(
    "statistics.epoch",
    2,
    StatisticsEpochPayload.serializer(),
    Absence.KEEP,
) {
    override val oldestReadableVersion = 1

    override val dataGroup = DataGroup.STATISTICS

    override fun profileOf(payload: StatisticsEpochPayload) = payload.profileUuid

    override val capture = listOf(
        CaptureTrigger("statistics_epochs", Event.INSERT, listOf("NEW.profile_id")),
        CaptureTrigger(
            "statistics_epochs",
            Event.UPDATE,
            listOf("NEW.profile_id"),
            fields = listOf(Field(2, "started_at"), Field(3, "cleared_at")),
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
            StatisticsEpochPayload(
                profileUuid = profileUuid,
                startedAt = stamps.field(2, row.started_at, previous?.startedAt),
                clearedAt = stamps.field(3, row.cleared_at, previous?.clearedAt),
            )
        }
    }

    override fun merge(local: StatisticsEpochPayload, incoming: StatisticsEpochPayload) = local.copy(
        startedAt = earlier(local.startedAt, incoming.startedAt),
        clearedAt = clearedAt(local.clearedAt, incoming.clearedAt),
    )

    // A clear is never undone, only moved back by restoring a backup made before it. A device that recorded its first
    // activity without having heard of a clear inserts its row with none, and that insert must not erase the clear.
    private fun clearedAt(a: Stamped<Long?>, b: Stamped<Long?>): Stamped<Long?> = when {
        a.value == null && b.value != null -> b
        b.value == null && a.value != null -> a
        else -> latest(a, b)
    }

    override suspend fun apply(writes: LocalWrites, key: String, payload: StatisticsEpochPayload): ApplyOutcome {
        val profileId = writes.profileId(payload.profileUuid).idOr { return it }
        writes.database.sync_applyQueries.upsertStatisticsEpoch(
            profileId = profileId,
            startedAt = payload.startedAt.value,
            clearedAt = payload.clearedAt.value,
        )
        return ApplyOutcome.Applied(LocalRef.of(profileId))
    }
}
