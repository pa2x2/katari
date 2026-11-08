package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Keyboard
import app.katari.icons.materialsymbols.rounded.Mouse
import app.katari.icons.materialsymbols.rounded.SwipeVertical
import app.katari.presentation.kit.gesture.InputGuide
import app.katari.presentation.kit.gesture.InputGuideHint
import eu.kanade.tachiyomi.ui.video.player.components.videoPlayerSafeContentPadding
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The basics of the mouse and keys, as Android's guide shows its gestures: the clicks, the wheel, and the keys most
 * reached for, with ? for the rest.
 */
@Composable
internal fun DesktopVideoPlayerGuide(seekStepSeconds: Int, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    InputGuide(
        hints = listOf(
            InputGuideHint(
                icon = MaterialSymbols.Rounded.Mouse,
                title = stringResource(MR.strings.anime_guide_click),
                details = listOf(
                    stringResource(MR.strings.anime_guide_double_click),
                    stringResource(MR.strings.anime_guide_hold),
                    stringResource(MR.strings.anime_guide_right_click),
                ),
            ),
            InputGuideHint(
                icon = MaterialSymbols.Rounded.SwipeVertical,
                title = stringResource(MR.strings.anime_guide_scroll_volume),
                details = listOf(
                    stringResource(MR.strings.anime_guide_scroll_seek),
                    stringResource(MR.strings.anime_guide_scroll_zoom),
                ),
            ),
            InputGuideHint(
                icon = MaterialSymbols.Rounded.Keyboard,
                title = stringResource(MR.strings.anime_guide_keys_play),
                details = listOf(
                    stringResource(MR.strings.anime_guide_keys_seek, seekStepSeconds),
                    stringResource(MR.strings.anime_guide_keys_list),
                ),
            ),
        ),
        onDismiss = onDismiss,
        modifier = modifier,
        safeArea = Modifier.videoPlayerSafeContentPadding(includeTop = true, includeBottom = true),
    )
}
