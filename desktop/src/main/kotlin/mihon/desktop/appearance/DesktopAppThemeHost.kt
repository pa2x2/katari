package mihon.desktop.appearance

import dev.zacsweers.metro.Inject
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.presentation.more.settings.screen.appearance.AppThemeHost
import mihon.core.platform.PlatformContext

/**
 * Desktop takes no colours from the wallpaper. Its windows keep their content and the kit theme follows the theme
 * preferences as they change, the light or dark choice included, so nothing besides the preference holds a theme mode
 * and nothing needs rebuilding.
 */
@Inject
class DesktopAppThemeHost : AppThemeHost {
    override val isDynamicColorAvailable: Boolean = false

    override fun applyThemeMode(mode: ThemeMode) = Unit

    override fun rebuildForTheme(context: PlatformContext) = Unit
}
