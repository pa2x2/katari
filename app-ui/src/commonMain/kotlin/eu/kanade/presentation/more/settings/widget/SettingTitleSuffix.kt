package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable

/** What follows a setting's title before its mark: the active profile's chip where the setting is the profile's. */
internal fun settingTitleSuffix(isProfileSpecific: Boolean): (@Composable () -> Unit)? =
    if (isProfileSpecific) {
        { ProfileSpecificChip() }
    } else {
        null
    }
