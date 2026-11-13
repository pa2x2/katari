package app.katari.sync.client.part

import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.scope.SettingsArea
import app.katari.sync.client.setting.value.LocalIdPreferences

/**
 * What this device chose to sync, as one snapshot: the data groups, Settings areas and profiles it keeps to itself, the
 * settings and menus' settings it keeps out of a synced area, and the settings it syncs that stay on each device by
 * default.
 * [profilesOff] includes the profiles waiting for the person to say whether they come to this device, and
 * [askBeforeNewProfiles] makes every profile this device never had wait so.
 */
data class ChoiceSet(
    val dataOff: Set<DataGroup> = emptySet(),
    val areasOff: Set<SettingsArea> = emptySet(),
    val settingsOff: Set<String> = emptySet(),
    val menusOff: Set<MenuSettings> = emptySet(),
    val settingsOn: Set<String> = emptySet(),
    val profilesOff: Set<String> = emptySet(),
    val askBeforeNewProfiles: Boolean = false,
) {
    fun syncs(group: DataGroup): Boolean = group !in dataOff

    fun syncs(area: SettingsArea): Boolean = area.followsData?.let(::syncs) ?: (area !in areasOff)

    /** Whether [menu]'s settings sync where their area does. */
    fun syncs(menu: MenuSettings): Boolean = menu !in menusOff

    /**
     * Whether the preference [key] syncs on this device: in an area that does, chosen to or synced by default, and, if
     * it names categories, with the categories.
     */
    fun syncsSetting(key: String): Boolean {
        val mark = PreferenceScopes.markOf(key) ?: return false
        val area = mark.area ?: return false
        if (!syncs(area)) return false
        if (keptWithCategories(key)) return false
        return when (mark.scope) {
            PreferenceScope.PERSON -> mark.menu?.let { it !in menusOff } ?: (key !in settingsOff)
            PreferenceScope.DEVICE_BY_DEFAULT -> key in settingsOn
            PreferenceScope.DEVICE -> false
        }
    }

    /** Whether [key] names categories while this device doesn't sync them, which keeps it here whatever it chose. */
    fun keptWithCategories(key: String): Boolean =
        LocalIdPreferences.namesCategories(key) && !syncs(DataGroup.CATEGORIES)

    fun syncsProfile(uuid: String): Boolean = uuid !in profilesOff

    /**
     * These choices with [key] synced: brought back into its area, for a setting synced by default, or chosen to sync,
     * for one that stays on each device by default.
     */
    fun withSettingSynced(key: String): ChoiceSet = when (PreferenceScopes.markOf(key)?.scope) {
        PreferenceScope.PERSON -> copy(settingsOff = settingsOff - key)
        PreferenceScope.DEVICE_BY_DEFAULT -> copy(settingsOn = settingsOn + key)
        PreferenceScope.DEVICE, null -> throw IllegalArgumentException("$key never syncs")
    }
}

/** The values of [entries] named here; a name a later version wrote and this one doesn't know chooses nothing. */
internal fun <E : Enum<E>> Collection<String>.enumsNamed(entries: List<E>): Set<E> =
    mapNotNullTo(HashSet()) { name -> entries.firstOrNull { it.name == name } }
