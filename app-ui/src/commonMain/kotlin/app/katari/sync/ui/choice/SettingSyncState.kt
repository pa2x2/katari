package app.katari.sync.ui.choice

import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.setting.scope.DeviceBound
import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.scope.SettingsArea

/** What a setting's sync popover offers, from how the setting syncs and what this device chose. */
internal sealed interface SettingSyncState {
    /**
     * The setting [key], with a switch of its own. One [syncedByDefault] syncs with its area unless kept out of it; any
     * other stays on each device unless chosen to sync.
     */
    data class Switch(val key: String, val area: SettingsArea, val on: Boolean, val syncedByDefault: Boolean) :
        SettingSyncState

    /** A setting set from [menu], whose settings sync or stay as one. */
    data class Menu(val area: SettingsArea, val menu: MenuSettings, val on: Boolean) : SettingSyncState

    /** A setting whose whole area this device doesn't sync. */
    data class AreaOff(val area: SettingsArea) : SettingSyncState

    /** A setting that syncs or stays with [group], such as the daily goal with the statistics. */
    data class WithData(val group: DataGroup, val synced: Boolean) : SettingSyncState

    /** A setting that only works on this device, for the reason [bound] gives. */
    data class DeviceOnly(val area: SettingsArea?, val bound: DeviceBound) : SettingSyncState
}

/** The state of the setting [key] under [choices]; null for a key no scope table marks, or one about syncing itself. */
internal fun settingSyncState(key: String, choices: ChoiceSet): SettingSyncState? {
    val mark = PreferenceScopes.markOf(key) ?: return null
    if (mark.bound == DeviceBound.SYNC) return null
    mark.bound?.let { return SettingSyncState.DeviceOnly(mark.area, it) }
    val area = mark.area ?: return null
    area.followsData?.let { return SettingSyncState.WithData(it, choices.syncs(it)) }
    if (!choices.syncs(area)) return SettingSyncState.AreaOff(area)
    if (choices.keptWithCategories(key)) return SettingSyncState.WithData(DataGroup.CATEGORIES, synced = false)
    mark.menu?.let { return menuSyncState(it, choices) }
    return when (mark.scope) {
        PreferenceScope.PERSON -> SettingSyncState.Switch(
            key,
            area,
            key !in choices.settingsOff,
            syncedByDefault = true,
        )
        PreferenceScope.DEVICE_BY_DEFAULT -> SettingSyncState.Switch(key, area, key in choices.settingsOn, false)
        PreferenceScope.DEVICE -> null
    }
}

/** The state of [menu]'s settings under [choices], which sync or stay as one. */
internal fun menuSyncState(menu: MenuSettings, choices: ChoiceSet): SettingSyncState {
    val area = PreferenceScopes.areaOf(menu)
    if (!choices.syncs(area)) return SettingSyncState.AreaOff(area)
    return SettingSyncState.Menu(area, menu, choices.syncs(menu))
}
