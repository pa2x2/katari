package eu.kanade.presentation.more.settings.screen.browse.longpress

import dev.icerock.moko.resources.StringResource
import mihon.core.common.CustomPreferences.BrowseLongPressAction
import tachiyomi.i18n.*

internal val BrowseLongPressAction.titleRes: StringResource
    get() = when (this) {
        BrowseLongPressAction.LIBRARY_ACTION -> MR.strings.pref_browse_long_press_action_library_action
        BrowseLongPressAction.PREVIEW -> MR.strings.pref_browse_long_press_action_preview
        BrowseLongPressAction.IMMERSIVE -> MR.strings.pref_browse_long_press_action_immersive
    }
