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
import app.katari.sync.client.setting.apply.LocalSetting
import app.katari.sync.client.setting.apply.localSetting
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.value.encodeSetting

/**
 * A profile's preferences of one [scope], one record per key, so edits of different keys on two devices both stay and
 * edits of one key take the latest. Keyed by profile uuid and the encoded key. Local ref: `[profile ID, key]`.
 * Preferences aren't in the database: their edits come from [app.katari.sync.client.setting.capture.SettingsCapture].
 * A preference that is no longer set travels as an empty value, never as a deletion.
 */
sealed class ProfileSettings(name: String, private val scope: PreferenceScope) :
    SyncedKind<ProfileSettingPayload>(name, 1, ProfileSettingPayload.serializer(), Absence.KEEP) {
    override fun syncsOn(choices: ChoiceSet, payload: ProfileSettingPayload) = choices.syncsSetting(payload.key)

    override fun settingKey(payload: ProfileSettingPayload) = payload.key

    override fun profileOf(payload: ProfileSettingPayload) = payload.profileUuid

    override val capture: List<CaptureTrigger> = emptyList()

    fun ref(profileId: Long, key: String): LocalRef = LocalRef.of(profileId, key)

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<ProfileSettingPayload> {
        val profileId = ref.long(0)
        val key = ref.text(1)
        if (PreferenceScopes.of(key) != scope) return LocalRead.NotSynced
        val profileUuid = reads.profileUuid(profileId) ?: return LocalRead.Missing
        val setting = reads.encodeSetting(key, reads.settings.profileValue(profileId, key), profileId)
        return LocalRead.Found(
            key = RecordKeys.derive(name, profileUuid, setting.key, *setting.keySourceParts),
            parents = listOf(RecordRef(ProfileKind, ProfileKind.ref(profileUuid))) + setting.parents,
        ) { previous, stamps ->
            ProfileSettingPayload(
                profileUuid = profileUuid,
                key = setting.key,
                keySources = setting.keySources,
                value = stamps.field(4, setting.value, previous?.value),
            )
        }
    }

    override fun merge(local: ProfileSettingPayload, incoming: ProfileSettingPayload) =
        local.copy(value = latest(local.value, incoming.value))

    override suspend fun apply(writes: LocalWrites, key: String, payload: ProfileSettingPayload): ApplyOutcome {
        val profileId = writes.profileId(payload.profileUuid).idOr { return it }
        val setting = writes.localSetting(payload.key, payload.keySources, payload.value.value, payload.profileUuid)
        if (setting !is LocalSetting.Ready) return (setting as LocalSetting.NotReady).outcome
        writes.settings.writeProfileValue(profileId, setting.key, setting.value)
        return ApplyOutcome.Applied(ref(profileId, setting.key))
    }

    companion object {
        /** The kind a profile's preference [key] syncs as, or null for one that never syncs. */
        fun of(key: String): ProfileSettings? = when (PreferenceScopes.of(key)) {
            PreferenceScope.PERSON -> ProfileSettingKind
            PreferenceScope.DEVICE_BY_DEFAULT -> ChosenProfileSettingKind
            PreferenceScope.DEVICE -> null
        }
    }
}

/** A profile's person-level preferences. */
object ProfileSettingKind : ProfileSettings("setting.profile", PreferenceScope.PERSON)

/**
 * A profile's preferences that stay on each device by default, from the devices that chose to sync them. A kind of its
 * own, so an app that doesn't know such a choice holds them as unreadable rather than writing over its own value.
 */
object ChosenProfileSettingKind : ProfileSettings("setting.profile.chosen", PreferenceScope.DEVICE_BY_DEFAULT)
