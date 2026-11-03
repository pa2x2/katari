package mihon.feature.profiles.ui.switcher

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import mihon.feature.profiles.core.Profile

/**
 * How profile UI on the home screen reaches the profile switcher. [LocalProfileSwitcher] is null outside the home
 * screen and with a single visible profile, where there is nothing to switch to and no profile UI is shown.
 */
@Immutable
class ProfileSwitcher(
    val activeProfile: Profile,
    val profileCount: Int,
    val open: () -> Unit,
)

val LocalProfileSwitcher: ProvidableCompositionLocal<ProfileSwitcher?> = compositionLocalOf { null }
