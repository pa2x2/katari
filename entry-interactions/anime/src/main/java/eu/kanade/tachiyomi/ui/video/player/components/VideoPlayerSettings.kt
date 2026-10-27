package eu.kanade.tachiyomi.ui.video.player.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import mihon.entry.viewer.chrome.ui.OverlayDrawer
import mihon.entry.viewer.chrome.ui.OverlayDrawerHeader
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Settings
import mihon.presentation.kit.list.ProvideRowDensity
import mihon.presentation.kit.list.RowDensity
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Where the playback settings open. In landscape a bottom sheet leaves room for about two rows and, being a dialog
 * window, brings the system bars back over the video, so they open as a side panel like the episode list instead.
 * Portrait keeps the bottom sheet.
 */
@Composable
internal fun VideoPlayerSettings(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable (VideoPlayerSettingsFrame, Modifier) -> Unit,
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        OverlayDrawer(
            visible = visible,
            onDismissRequest = onDismissRequest,
            drawerModifier = Modifier.videoPlayerSafeContentPadding(includeTop = true, includeBottom = true),
        ) {
            ProvideRowDensity(RowDensity.Compact) {
                content(VideoPlayerSettingsFrame.Panel, Modifier.fillMaxSize())
            }
        }
    } else if (visible) {
        Sheet(onDismissRequest = onDismissRequest, capHeight = true) {
            content(VideoPlayerSettingsFrame.Sheet, Modifier)
        }
    }
}

/** What holds the settings, which decides the header they draw. */
internal enum class VideoPlayerSettingsFrame { Sheet, Panel }

/**
 * The settings' title as their [frame] draws it, with [menu] behind the overflow button when given. A page opened from
 * the settings passes its own [title] and [onBack], which returns to them.
 */
@Composable
internal fun VideoPlayerSettingsHeader(
    frame: VideoPlayerSettingsFrame,
    onDismissRequest: () -> Unit,
    menu: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
    title: String = stringResource(MR.strings.action_settings),
    onBack: (() -> Unit)? = null,
) {
    when (frame) {
        VideoPlayerSettingsFrame.Sheet -> SheetHeader(
            title = title,
            menu = menu,
            onBack = onBack,
        )
        VideoPlayerSettingsFrame.Panel -> OverlayDrawerHeader(
            title = title,
            icon = MaterialSymbols.Rounded.Settings,
            closeDescription = stringResource(MR.strings.action_close),
            onClose = onDismissRequest,
            menu = menu,
            onBack = onBack,
        )
    }
}
