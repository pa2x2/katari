package mihon.desktop.appearance

import dev.zacsweers.metro.Inject
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.presentation.more.settings.screen.appearance.AppThemeHost
import mihon.core.platform.PlatformContext

/**
 * Desktop takes no colours from the wallpaper and keeps no theme mode besides the preference. Its window can't rebuild
 * its content yet, so a theme picked in settings shows from the next start, until the window rebuilds on a change.
 */
@Inject
class DesktopAppThemeHost : AppThemeHost {
    override val isDynamicColorAvailable: Boolean = false

    override fun applyThemeMode(mode: ThemeMode) = Unit

    override fun rebuildForTheme(context: PlatformContext) = Unit
}
