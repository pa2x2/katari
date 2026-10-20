package eu.kanade.tachiyomi.ui.video.player.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import mihon.entry.viewer.chrome.ui.OverlayDrawer
import mihon.presentation.kit.list.ProvideRowDensity
import mihon.presentation.kit.list.RowDensity
import mihon.presentation.kit.sheet.Sheet

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
