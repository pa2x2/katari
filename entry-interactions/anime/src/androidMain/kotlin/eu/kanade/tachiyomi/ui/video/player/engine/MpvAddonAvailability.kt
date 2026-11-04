package eu.kanade.tachiyomi.ui.video.player.engine

import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.addon.MpvAddonState
import mihon.video.engine.mpv.addon.isFixedByUpdate

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
