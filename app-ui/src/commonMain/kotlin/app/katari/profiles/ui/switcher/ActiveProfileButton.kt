package app.katari.profiles.ui.switcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.navigation.AppBarAvatarButton
import app.katari.presentation.kit.sheet.popoverAnchor
import app.katari.presentation.kit.sheet.rememberPopoverAnchor
import app.katari.presentation.kit.surface.AvatarSize
import app.katari.profiles.ui.components.ProfileAvatar
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The active profile's avatar at the end of a top-level app bar, opening the switcher; absent with one profile. */
@Composable
fun ActiveProfileButton() {
    val switcher = LocalProfileSwitcher.current ?: return
    val anchor = rememberPopoverAnchor()
    AppBarAvatarButton(
        contentDescription = stringResource(MR.strings.profiles_current, switcher.activeProfile.name),
        onClick = { switcher.open(anchor) },
        modifier = Modifier.popoverAnchor(anchor),
    ) {
        ProfileAvatar(colorSeed = switcher.activeProfile.colorSeed, size = AvatarSize.Bar)
    }
}
