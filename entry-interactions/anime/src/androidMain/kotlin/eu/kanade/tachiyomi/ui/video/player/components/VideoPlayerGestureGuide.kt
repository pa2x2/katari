package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.SwipeVertical
import mihon.icons.materialsymbols.rounded.TouchApp
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
    VideoPlayerInputGuide(
        hints = listOf(
            VideoPlayerGuideHint(
                icon = MaterialSymbols.Rounded.SwipeVertical,
                title = stringResource(MR.strings.anime_guide_brightness),
                details = listOf(stringResource(MR.strings.anime_guide_double_tap_back, seekStepSeconds)),
            ),
            VideoPlayerGuideHint(
                icon = MaterialSymbols.Rounded.TouchApp,
                title = stringResource(MR.strings.anime_guide_hold),
                details = listOf(
                    stringResource(MR.strings.anime_guide_swipe_seek),
                    stringResource(MR.strings.anime_guide_tap),
                ),
            ),
            VideoPlayerGuideHint(
                icon = MaterialSymbols.Rounded.SwipeVertical,
                title = stringResource(MR.strings.anime_guide_volume),
                details = listOf(stringResource(MR.strings.anime_guide_double_tap_forward, seekStepSeconds)),
            ),
        ),
        onDismiss = onDismiss,
        modifier = modifier,
    )
}
