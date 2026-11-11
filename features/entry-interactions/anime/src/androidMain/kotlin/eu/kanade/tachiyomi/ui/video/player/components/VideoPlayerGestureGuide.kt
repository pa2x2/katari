package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.SwipeVertical
import app.katari.platform.presentation.platformPresentation
import app.katari.presentation.kit.gesture.InputGuide
import app.katari.presentation.kit.gesture.InputGuideHint
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Labels the player's touch areas the first time it opens in a profile: the brightness and volume halves with their
 * double-tap seeks, and in the middle hold for 2×, a sideways swipe to seek and a tap for the controls.
 */
@Composable
internal fun VideoPlayerGestureGuide(
    seekStepSeconds: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InputGuide(
        hints = listOf(
            InputGuideHint(
                icon = MaterialSymbols.Rounded.SwipeVertical,
                title = stringResource(PlatformStrings.anime_guide_brightness),
                details = listOf(stringResource(PlatformStrings.anime_guide_double_tap_back, seekStepSeconds)),
            ),
            InputGuideHint(
                icon = platformPresentation.pointerIcon,
                title = stringResource(MR.strings.anime_guide_hold),
                details = listOf(
                    stringResource(PlatformStrings.anime_guide_swipe_seek),
                    stringResource(PlatformStrings.anime_guide_tap),
                ),
            ),
            InputGuideHint(
                icon = MaterialSymbols.Rounded.SwipeVertical,
                title = stringResource(PlatformStrings.anime_guide_volume),
                details = listOf(stringResource(PlatformStrings.anime_guide_double_tap_forward, seekStepSeconds)),
            ),
        ),
        onDismiss = onDismiss,
        modifier = modifier,
        safeArea = Modifier.videoPlayerSafeContentPadding(includeTop = true, includeBottom = true),
    )
}
