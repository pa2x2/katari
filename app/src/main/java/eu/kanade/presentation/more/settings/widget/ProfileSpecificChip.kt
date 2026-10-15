package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ProfileSpecificChip() {
    Pill(
        text = stringResource(MR.strings.profile_specific_chip),
        container = SurfaceContainer.SecondaryContainer,
        role = TextRole.Badge,
    )
}

internal fun profileSpecificSuffix(isProfileSpecific: Boolean): (@Composable () -> Unit)? =
    if (isProfileSpecific) {
        { ProfileSpecificChip() }
    } else {
        null
    }
