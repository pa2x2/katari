package app.katari.sync.client.firstsync

import app.katari.sync.client.kind.setting.ProfileSettingKind
import app.katari.sync.client.kind.setting.SharedSettingKind
import app.katari.sync.client.kind.setting.SourceSettingKind
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.setting.read.SettingReads
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes

/**
 * The person-level settings a profile holds, as the setting records they are (kind name and local ref): its own
 * preferences and its sources' settings, and for the default profile also the settings every profile shares.
 */
internal fun SettingReads.personLevelSettings(
    profileId: Long,
    isDefaultProfile: Boolean,
): List<Pair<String, LocalRef>> =
    buildList {
        profileKeys(profileId).filter { it.isPersonLevel() }.forEach { key ->
            add(ProfileSettingKind.name to ProfileSettingKind.ref(profileId, key))
        }
        sourceKeys(profileId).forEach { (sourceId, keys) ->
            keys.forEach { add(SourceSettingKind.name to SourceSettingKind.ref(profileId, sourceId, it)) }
        }
        if (isDefaultProfile) {
            sharedKeys().filter { it.isPersonLevel() && !isProfileKey(it) }.forEach { key ->
                add(SharedSettingKind.name to SharedSettingKind.ref(key))
            }
        }
    }

internal fun String.isPersonLevel() = PreferenceScopes.of(this) == PreferenceScope.PERSON
