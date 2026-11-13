package app.katari.sync.client.arrival

import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.client.setting.scope.PreferenceScopes

/**
 * A setting another device changed here: [key] in [profileId]'s store, or the shared one when null, changed at
 * [changedAt] on the device named [from] (null when the account doesn't list it any more). The changes one sync applied
 * share [batch].
 */
data class ArrivedSetting(
    val id: Long,
    val profileId: Long?,
    val key: String,
    val from: String?,
    val changedAt: Long,
    val batch: Long,
    val keepMine: KeepMine,
) {
    /** The menu whose settings [key] syncs with, whose arrival is told and kept as one. */
    val menu: MenuSettings?
        get() = PreferenceScopes.markOf(key)?.menu
}

/** What Keep mine can do for an arrived setting, or did, as this device's choices stand now. */
enum class KeepMine {
    /** Not used yet: it can put back this device's value and keep the setting here. */
    OFFERED,

    /** Used, and this device still keeps the setting to itself. */
    KEPT_HERE,

    /** Used, but the setting syncs again since, so what it kept may have been replaced. */
    SYNCED_AGAIN,
}
