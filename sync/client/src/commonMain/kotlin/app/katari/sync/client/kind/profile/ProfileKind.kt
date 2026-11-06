package app.katari.sync.client.kind.profile

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.HoldReason
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.merge.latest
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.SyncedKind

/** Profiles, keyed by their uuid, which is random for every profile but `default`. Local ref: `[uuid]`. */
object ProfileKind : SyncedKind<ProfilePayload>("profile", 1, ProfilePayload.serializer(), Absence.TOMBSTONE) {
    override val capture = listOf(
        CaptureTrigger("profiles", Event.INSERT, listOf("NEW.uuid")),
        CaptureTrigger(
            "profiles",
            Event.UPDATE,
            listOf("NEW.uuid"),
            fields = listOf(
                Field(2, "name"),
                Field(3, "color_seed"),
                Field(4, "position"),
                Field(5, "is_archived"),
            ),
        ),
        CaptureTrigger("profiles", Event.DELETE, listOf("OLD.uuid")),
    )

    fun key(uuid: String): String = RecordKeys.derive(name, uuid)

    fun ref(uuid: String): LocalRef = LocalRef.of(uuid)

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<ProfilePayload> {
        val row = reads.database.sync_local_rowsQueries.profileRow(ref.text(0)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        return LocalRead.Found(key(row.uuid)) { previous, stamps ->
            ProfilePayload(
                uuid = row.uuid,
                name = stamps.field(2, row.name, previous?.name),
                colorSeed = stamps.field(3, row.color_seed, previous?.colorSeed),
                position = stamps.field(4, row.position, previous?.position),
                archived = stamps.field(5, row.is_archived, previous?.archived),
            )
        }
    }

    override fun merge(local: ProfilePayload, incoming: ProfilePayload) = local.copy(
        name = latest(local.name, incoming.name),
        colorSeed = latest(local.colorSeed, incoming.colorSeed),
        position = latest(local.position, incoming.position),
        archived = latest(local.archived, incoming.archived),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: ProfilePayload): ApplyOutcome {
        val queries = writes.database.sync_applyQueries
        if (queries.profileId(payload.uuid).awaitAsOneOrNull() == null) {
            queries.insertProfile(
                uuid = payload.uuid,
                name = payload.name.value,
                colorSeed = payload.colorSeed.value,
                position = payload.position.value,
                isArchived = payload.archived.value,
            )
        } else {
            queries.updateProfile(
                name = payload.name.value,
                colorSeed = payload.colorSeed.value,
                position = payload.position.value,
                isArchived = payload.archived.value,
                uuid = payload.uuid,
            )
        }
        return ApplyOutcome.Applied(ref(payload.uuid))
    }

    /**
     * Deletes the profile with everything in it, as deleting it here does, but for its files: downloads of its
     * entries stay until they are cleaned up. The default profile is never deleted, and the profile open here waits
     * until another one is.
     */
    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        val uuid = ref?.text(0) ?: return ApplyOutcome.Applied(null)
        if (uuid == DEFAULT_UUID) return ApplyOutcome.Applied(ref)
        val queries = writes.database.sync_applyQueries
        val profileId = queries.profileId(uuid).awaitAsOneOrNull() ?: return ApplyOutcome.Applied(null)
        if (profileId == writes.activeProfileId) return ApplyOutcome.Held(HoldReason.PROFILE_OPEN)
        queries.deleteProfile(uuid)
        writes.settings.deleteProfile(profileId)
        return ApplyOutcome.Applied(null)
    }

    const val DEFAULT_UUID = "default"
}
