package app.katari.sync.client.kind.setting

import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.merge.latest
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.client.setting.scope.SettingsArea
import app.katari.sync.client.setting.value.SettingValue

/**
 * Settings sources keep for a profile, one record per key, keyed by profile uuid, the source's own ID and the key.
 * Local ref: `[profile ID, source ID, key]`. Sources keep logins in them too, which sync as automatic backups already
 * carry them. A source's keys can depend on each other, but only the source knows how, so each key takes its latest
 * edit like any other setting.
 */
object SourceSettingKind : SyncedKind<SourceSettingPayload>(
    "setting.source",
    1,
    SourceSettingPayload.serializer(),
    Absence.KEEP,
) {
    override fun syncsOn(choices: ChoiceSet, payload: SourceSettingPayload) = choices.syncs(settingsArea(payload))

    override fun profileOf(payload: SourceSettingPayload) = payload.profileUuid

    override fun settingsArea(payload: SourceSettingPayload) = SettingsArea.EXTENSION_SETTINGS

    override val capture: List<CaptureTrigger> = emptyList()

    fun ref(profileId: Long, sourceId: Long, key: String): LocalRef = LocalRef.of(profileId, sourceId, key)

    /** What the refs of every key of [profileId]'s settings of [sourceId] start with. */
    fun refPrefix(profileId: Long, sourceId: Long): String = LocalRef.of(profileId, sourceId).json.dropLast(1) + ","

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<SourceSettingPayload> {
        val profileId = ref.long(0)
        val sourceId = ref.long(1)
        val key = ref.text(2)
        val profileUuid = reads.profileUuid(profileId) ?: return LocalRead.Missing
        val value = SettingValue.of(reads.settings.sourceValue(profileId, sourceId, key))
        return LocalRead.Found(
            key = RecordKeys.derive(name, profileUuid, sourceId.toString(), key),
            parents = listOf(RecordRef(ProfileKind, ProfileKind.ref(profileUuid))),
        ) { previous, stamps ->
            SourceSettingPayload(
                profileUuid = profileUuid,
                sourceId = sourceId,
                key = key,
                value = stamps.field(4, value, previous?.value),
            )
        }
    }
    override fun merge(local: SourceSettingPayload, incoming: SourceSettingPayload) =
        local.copy(value = latest(local.value, incoming.value))

    override suspend fun apply(writes: LocalWrites, key: String, payload: SourceSettingPayload): ApplyOutcome {
        val profileId = writes.profileId(payload.profileUuid).idOr { return it }
        writes.settings.writeSourceValue(profileId, payload.sourceId, payload.key, payload.value.value.stored)
        return ApplyOutcome.Applied(ref(profileId, payload.sourceId, payload.key))
    }
}
