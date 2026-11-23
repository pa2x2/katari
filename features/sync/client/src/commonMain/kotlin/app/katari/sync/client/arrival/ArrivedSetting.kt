package app.katari.sync.client.arrival

import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.client.setting.scope.PreferenceScopes

/**
 * A setting another device changed here: [key] in [profileId]'s store, or the shared one when null, changed on the
 * device named [from] (null when the account doesn't list it any more). The changes one sync applied share [batch],
 * the time that sync ran in epoch milliseconds.
 */
data class ArrivedSetting(
    val id: Long,
    val profileId: Long?,
    val key: String,
    val from: String?,
    val batch: Long,
    val keepMine: KeepMine,
    /** What the other devices changed; null on a change noted before the app kept the value it arrived with. */
    val change: ArrivedChange?,
) {
    /** The menu whose settings [key] syncs with, whose arrival is told and kept as one. */
    val menu: MenuSettings?
        get() = PreferenceScopes.markOf(key)?.menu
}

/**
 * The value this device had before the other devices changed the setting, which Keep mine puts back, and the value
 * their last change wrote, both as the store holds them: with this device's source keys and record IDs, and null for a
 * setting that isn't set and reads as its default.
 */
data class ArrivedChange(val before: Any?, val after: Any?)

/** What Keep mine can do for an arrived setting, or did, as this device's choices stand now. */
enum class KeepMine {
    /** Not used yet: it can put back this device's value and keep the setting here. */
    OFFERED,

    /** Used, and this device still keeps the setting to itself. */
    KEPT_HERE,

    /** Used, but the setting syncs again since, so what it kept may have been replaced. */
    SYNCED_AGAIN,
}
