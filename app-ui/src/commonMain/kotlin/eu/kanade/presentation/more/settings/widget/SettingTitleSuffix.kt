package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import app.katari.sync.ui.choice.SettingSyncMarkIcon
import eu.kanade.presentation.more.settings.LocalSettingSyncMark

/**
 * What follows a setting's title: the active profile's chip where the setting is the profile's, and its sync mark
 * while this device is linked.
 */
@Composable
internal fun settingTitleSuffix(isProfileSpecific: Boolean): (@Composable () -> Unit)? {
    val syncMark = LocalSettingSyncMark.current
    if (!isProfileSpecific && syncMark == null) return null
    return {
        if (isProfileSpecific) ProfileSpecificChip()
        syncMark?.let { SettingSyncMarkIcon(it) }
    }
}
