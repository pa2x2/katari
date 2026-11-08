package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.presentation.kit.surface.AvatarSize
import app.katari.presentation.kit.surface.Pill
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.TextRole
import app.katari.profiles.ui.components.ProfileAvatar
import app.katari.ui.di.LocalAppUiGraph
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Marks a setting that belongs to the active profile, naming that profile once there is more than one. */
@Composable
fun ProfileSpecificChip() {
    val profileManager = LocalAppUiGraph.current.profileManager
    val activeProfile by profileManager.activeProfile.collectAsState()
    val visibleProfiles by profileManager.visibleProfiles.collectAsState()
    val namedProfile = activeProfile?.takeIf { visibleProfiles.size > 1 }
    Pill(
        text = namedProfile?.name ?: stringResource(MR.strings.profile_specific_chip),
        container = SurfaceContainer.SecondaryContainer,
        role = TextRole.Badge,
        leading = namedProfile?.let { { ProfileAvatar(colorSeed = it.colorSeed, size = AvatarSize.Inline) } },
    )
}

internal fun profileSpecificSuffix(isProfileSpecific: Boolean): (@Composable () -> Unit)? =
    if (isProfileSpecific) {
        { ProfileSpecificChip() }
    } else {
        null
    }
