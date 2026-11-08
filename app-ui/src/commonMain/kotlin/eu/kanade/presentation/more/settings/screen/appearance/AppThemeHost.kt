package eu.kanade.presentation.more.settings.screen.appearance

import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.ThemeMode

/** How the platform puts the theme the user picks into effect, beyond storing it. */
@PlatformFacility
interface AppThemeHost {
    /**
     * Whether colours taken from the system, such as the wallpaper's or the desktop's accent, [AppTheme.MONET], are
     * offered; the theme is hidden otherwise.
     */
    val isDynamicColorAvailable: Boolean

    /** Hands [mode] to the platform where it keeps a theme mode of its own besides the preference. */
    fun applyThemeMode(mode: ThemeMode)

    /** Rebuilds the UI around [context], which reads the theme and pure black only when it is built. */
    fun rebuildForTheme(context: PlatformContext)
}
