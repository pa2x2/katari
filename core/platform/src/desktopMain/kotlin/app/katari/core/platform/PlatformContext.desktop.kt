package app.katari.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

actual abstract class PlatformContext

/** Provided by the desktop application around each window's content. */
val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> = staticCompositionLocalOf {
    error("No PlatformContext provided around this window's content")
}

@Composable
actual fun currentPlatformContext(): PlatformContext = LocalPlatformContext.current
