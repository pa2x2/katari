package eu.kanade.tachiyomi.ui.video.player.engine

import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.addon.MpvAddonState
import mihon.video.engine.mpv.addon.isFixedByUpdate

/** Whether the player can use mpv now, or what it would take. */
internal enum class MpvAvailability {
    Available,
    NotInstalled,

    /** Installed, but this Katari needs another release of it. */
    NeedsUpdate,

    /** Installed in a way no download fixes, such as signed with another key, or its libraries failed to load. */
    Unusable,
    ;

    val installable: Boolean
        get() = this == NotInstalled || this == NeedsUpdate
}

/** [state] is the add-on's state the caller observes, so that it recomposes when the add-on changes. */
internal fun MpvAddon.availability(state: MpvAddonState): MpvAvailability = when {
    isAvailable -> MpvAvailability.Available
    state is MpvAddonState.Installed -> if (state.problem?.isFixedByUpdate == true) {
        MpvAvailability.NeedsUpdate
    } else {
        MpvAvailability.Unusable
    }
    else -> MpvAvailability.NotInstalled
}
