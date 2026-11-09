package app.katari.sync.client.setting.read

import app.katari.sync.client.kind.setting.ProfileSettingKind
import app.katari.sync.client.kind.setting.SharedSettingKind
import app.katari.sync.client.kind.setting.SourceSettingKind
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.scope.SettingsArea

/**
 * The person-level settings a profile holds, as the setting records they are (kind name and local ref): its own
 * preferences and its sources' settings, and for the default profile also the settings every profile shares. Only
 * those of [area], when it is given.
 */
internal fun SettingReads.personLevelSettings(
    profileId: Long,
    isDefaultProfile: Boolean,
    area: SettingsArea? = null,
): List<Pair<String, LocalRef>> =
    buildList {
        fun String.counts() = isPersonLevel() && (area == null || PreferenceScopes.markOf(this)?.area == area)
        profileKeys(profileId).filter { it.counts() }.forEach { key ->
            add(ProfileSettingKind.name to ProfileSettingKind.ref(profileId, key))
        }
        if (area == null || area == SettingsArea.EXTENSION_SETTINGS) {
            sourceKeys(profileId).forEach { (sourceId, keys) ->
                keys.forEach { add(SourceSettingKind.name to SourceSettingKind.ref(profileId, sourceId, it)) }
            }
        }
        if (isDefaultProfile) {
            sharedKeys().filter { it.counts() && !isProfileKey(it) }.forEach { key ->
                add(SharedSettingKind.name to SharedSettingKind.ref(key))
            }
        }
    }

internal fun String.isPersonLevel() = PreferenceScopes.of(this) == PreferenceScope.PERSON
