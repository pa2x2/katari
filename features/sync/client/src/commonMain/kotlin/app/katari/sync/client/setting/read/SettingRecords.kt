package app.katari.sync.client.setting.read

import app.katari.sync.client.kind.setting.ProfileSettings
import app.katari.sync.client.kind.setting.SharedSettings
import app.katari.sync.client.kind.setting.SourceSettingKind
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes

/**
 * The settings a profile holds that can sync, as the setting records they are (kind name and local ref): its own
 * preferences [including] picks, its sources' settings when [withSources], and for the default profile also the
 * preferences every profile shares. Whether this device syncs each one is the drain's business.
 */
internal fun SettingReads.settingRecords(
    profileId: Long,
    isDefaultProfile: Boolean,
    including: (key: String) -> Boolean = { true },
    withSources: Boolean = true,
): List<Pair<String, LocalRef>> =
    buildList {
        profileKeys(profileId).filter(including).forEach { key ->
            ProfileSettings.of(key)?.let { add(it.name to it.ref(profileId, key)) }
        }
        if (withSources) {
            sourceKeys(profileId).forEach { (sourceId, keys) ->
                keys.forEach { add(SourceSettingKind.name to SourceSettingKind.ref(profileId, sourceId, it)) }
            }
        }
        if (isDefaultProfile) {
            sharedKeys().filter { including(it) && !isProfileKey(it) }.forEach { key ->
                SharedSettings.of(key)?.let { add(it.name to it.ref(key)) }
            }
        }
    }

internal fun String.isPersonLevel() = PreferenceScopes.of(this) == PreferenceScope.PERSON
