package mihon.entry.interactions.manga.media.immersive

import androidx.compose.runtime.Composable
import mihon.core.common.platform.PlatformFacility

/**
 * What a chapter in the immersive feed takes from the platform it shows on: Android keeps the screen on through the
 * window, desktop keeps it awake through the session while the window is seen.
 */
@PlatformFacility
internal interface MangaImmersivePlatform {
    /** Keeps the screen awake while [active], where the chapter is seen. */
    @Composable
    fun KeepAwake(active: Boolean)
}
