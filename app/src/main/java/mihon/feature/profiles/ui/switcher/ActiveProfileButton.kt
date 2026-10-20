package mihon.feature.profiles.ui.switcher

import androidx.compose.runtime.Composable
import mihon.feature.profiles.ui.components.ProfileAvatar
import mihon.presentation.kit.navigation.AppBarAvatarButton
import mihon.presentation.kit.surface.AvatarSize
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The active profile's avatar at the end of a top-level app bar, opening the switcher; absent with one profile. */
@Composable
internal fun ActiveProfileButton() {
    val switcher = LocalProfileSwitcher.current ?: return
    AppBarAvatarButton(
        contentDescription = stringResource(MR.strings.profiles_current, switcher.activeProfile.name),
        onClick = switcher.open,
    ) {
        ProfileAvatar(colorSeed = switcher.activeProfile.colorSeed, size = AvatarSize.Bar)
    }
}
