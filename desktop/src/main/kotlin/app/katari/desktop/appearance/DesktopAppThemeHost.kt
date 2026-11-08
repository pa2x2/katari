package app.katari.desktop.appearance

import app.katari.core.platform.PlatformContext
import app.katari.presentation.kit.theme.SystemAccentColor
import dev.zacsweers.metro.Inject
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.presentation.more.settings.screen.appearance.AppThemeHost

/**
 * Desktop takes the Dynamic theme's colours from the desktop's accent colour, so the theme is offered only while the
 * desktop publishes one. Its windows keep their content and the kit theme follows the theme preferences as they
 * change, the light or dark choice included, so nothing besides the preference holds a theme mode and nothing needs
 * rebuilding.
 */
@Inject
class DesktopAppThemeHost(private val accentColor: SystemAccentColor) : AppThemeHost {
    override val isDynamicColorAvailable: Boolean
        get() = accentColor.accentArgb.value != null

    override fun applyThemeMode(mode: ThemeMode) = Unit

    override fun rebuildForTheme(context: PlatformContext) = Unit
}
