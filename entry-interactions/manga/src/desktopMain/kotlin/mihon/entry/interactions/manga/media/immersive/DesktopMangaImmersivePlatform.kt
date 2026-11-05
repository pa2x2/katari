package mihon.entry.interactions.manga.media.immersive

import androidx.compose.runtime.Composable
import mihon.core.platform.power.ScreenIdleInhibitor
import mihon.entry.viewer.chrome.ui.window.KeepScreenAwake
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

internal class DesktopMangaImmersivePlatform(private val inhibitor: ScreenIdleInhibitor) : MangaImmersivePlatform {
    @Composable
    override fun KeepAwake(active: Boolean) = KeepScreenAwake(
        active = active,
        reason = stringResource(MR.strings.reader_keeps_screen_awake),
        inhibitor = inhibitor,
    )
}
