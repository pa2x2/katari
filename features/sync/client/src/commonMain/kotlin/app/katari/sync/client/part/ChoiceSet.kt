package app.katari.sync.client.part

import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.scope.SettingsArea
import app.katari.sync.client.setting.value.LocalIdPreferences

/**
 * What this device chose to sync, as one snapshot: the data groups, Settings areas and profiles it keeps to itself, the
 * settings it keeps out of a synced area, and the settings it syncs that stay on each device by default.
 * [profilesOff] includes the profiles waiting for the person to say whether they come to this device.
 */
data class ChoiceSet(
    val dataOff: Set<DataGroup> = emptySet(),
    val areasOff: Set<SettingsArea> = emptySet(),
    val settingsOff: Set<String> = emptySet(),
    val settingsOn: Set<String> = emptySet(),
    val profilesOff: Set<String> = emptySet(),
) {
    fun syncs(group: DataGroup): Boolean = group !in dataOff

    fun syncs(area: SettingsArea): Boolean = area.followsData?.let(::syncs) ?: (area !in areasOff)

    /**
     * Whether the preference [key] syncs on this device: in an area that does, chosen to or synced by default, and, if
     * it names categories, with the categories.
     */
    fun syncsSetting(key: String): Boolean {
        val mark = PreferenceScopes.markOf(key) ?: return false
        val area = mark.area ?: return false
        if (!syncs(area)) return false
        if (LocalIdPreferences.namesCategories(key) && !syncs(DataGroup.CATEGORIES)) return false
        return when (mark.scope) {
            PreferenceScope.PERSON -> key !in settingsOff
            PreferenceScope.DEVICE_BY_DEFAULT -> key in settingsOn
            PreferenceScope.DEVICE -> false
        }
    }

    fun syncsProfile(uuid: String): Boolean = uuid !in profilesOff
}
