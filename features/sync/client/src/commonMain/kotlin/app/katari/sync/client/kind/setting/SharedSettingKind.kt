package app.katari.sync.client.kind.setting

import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.merge.latest
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.client.setting.apply.LocalSetting
import app.katari.sync.client.setting.apply.localSetting
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.value.encodeSetting

/**
 * Person-level preferences of the store all profiles share, one record per key for the whole account. Local ref:
 * `[key]`. A key that belongs to a profile is never one: restoring a backup writes the default profile's preferences
 * into the shared store as well, and those are the profile's, synced as [ProfileSettingKind].
 */
object SharedSettingKind : SyncedKind<SharedSettingPayload>(
    "setting.shared",
    1,
    SharedSettingPayload.serializer(),
    Absence.KEEP,
) {
    override fun syncsOn(choices: ChoiceSet, payload: SharedSettingPayload) = choices.syncsSetting(payload.key)

    override fun settingKey(payload: SharedSettingPayload) = payload.key

    override val capture: List<CaptureTrigger> = emptyList()

    fun ref(key: String): LocalRef = LocalRef.of(key)

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<SharedSettingPayload> {
        val key = ref.text(0)
        if (PreferenceScopes.of(key) != PreferenceScope.PERSON || reads.settings.isProfileKey(key)) {
            return LocalRead.NotSynced
        }
        val setting = reads.encodeSetting(key, reads.settings.sharedValue(key), profileId = null)
        return LocalRead.Found(
            key = RecordKeys.derive(name, setting.key, *setting.keySourceParts),
            parents = setting.parents,
        ) { previous, stamps ->
            SharedSettingPayload(
                key = setting.key,
                keySources = setting.keySources,
                value = stamps.field(3, setting.value, previous?.value),
            )
        }
    }
    override fun merge(local: SharedSettingPayload, incoming: SharedSettingPayload) =
        local.copy(value = latest(local.value, incoming.value))

    override suspend fun apply(writes: LocalWrites, key: String, payload: SharedSettingPayload): ApplyOutcome {
        val setting = writes.localSetting(payload.key, payload.keySources, payload.value.value, profileUuid = null)
        if (setting !is LocalSetting.Ready) return (setting as LocalSetting.NotReady).outcome
        writes.settings.writeSharedValue(setting.key, setting.value)
        return ApplyOutcome.Applied(ref(setting.key))
    }
}
