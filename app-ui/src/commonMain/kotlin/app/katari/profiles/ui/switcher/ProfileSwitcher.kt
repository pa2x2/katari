package app.katari.profiles.ui.switcher

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import app.katari.presentation.kit.sheet.PopoverAnchor
import app.katari.profiles.core.Profile

/**
 * How profile UI on the home screen reaches the profile switcher. [LocalProfileSwitcher] is null outside the home
 * screen and with a single visible profile, where there is nothing to switch to and no profile UI is shown. [open] takes
 * the anchor of the button it was opened from, so the switcher opens beside it; the dock's Profiles button uses
 * [dockAnchor], which the dock marks, so the switcher opened from that destination (by click or shortcut) opens there.
 */
@Immutable
class ProfileSwitcher(
    val activeProfile: Profile,
    val profileCount: Int,
    val dockAnchor: PopoverAnchor,
    val open: (from: PopoverAnchor?) -> Unit,
)

val LocalProfileSwitcher: ProvidableCompositionLocal<ProfileSwitcher?> = compositionLocalOf { null }
