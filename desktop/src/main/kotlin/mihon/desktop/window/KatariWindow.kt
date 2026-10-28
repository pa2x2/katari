package mihon.desktop.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.platform.LocalPlatformContext
import mihon.desktop.di.DesktopGraph
import mihon.presentation.kit.content.ProvideTextRole
import mihon.presentation.kit.shortcut.KeyboardShortcutRegistry
import mihon.presentation.kit.shortcut.KeyboardShortcutsHost
import mihon.presentation.kit.surface.ContentOn
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalKatariThemeGraph
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.window.LocalScreenTitles
import mihon.presentation.kit.window.ScreenTitles
import tachiyomi.i18n.MR
import tachiyomi.i18n.app_name
import tachiyomi.i18n.window_title_with_screen
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The app's window: framed by the desktop, back where it was last time, titled with the app's name and the open
 * screen's title, and closing the app when it closes. [content] gets the graph, the theme and the window's platform
 * context, as an Android activity's content does, the window's keyboard shortcuts, which get the keys the content
 * leaves, and the window's screen titles, which the screens' app bars report to.
 */
@Composable
fun ApplicationScope.KatariWindow(graph: DesktopGraph, content: @Composable () -> Unit) {
    val state = rememberSavedWindowState(graph.windowPreferences)
    val shortcuts = remember { KeyboardShortcutRegistry() }
    val screenTitles = remember { ScreenTitles() }
    val appName = stringResource(MR.strings.app_name)
    val screenTitle = screenTitles.current
    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        title = screenTitle?.let { stringResource(MR.strings.window_title_with_screen, appName, it) } ?: appName,
        onKeyEvent = shortcuts::onKeyEvent,
    ) {
        val platformContext = remember(window) { WindowPlatformContext(window) }
        CompositionLocalProvider(
            LocalAppUiGraph provides graph,
            LocalKatariThemeGraph provides graph,
            LocalMetroViewModelFactory provides graph.viewModelFactory,
            LocalPlatformContext provides platformContext,
            LocalScreenTitles provides screenTitles,
        ) {
            KatariTheme {
                ContentOn(SurfaceContainer.Background) {
                    ProvideTextRole(TextRole.Supporting) {
                        KeyboardShortcutsHost(shortcuts) {
                            val backInput = rememberWindowBackInput()
                            WindowShortcuts(state = state, backInput = backInput, onQuit = ::exitApplication)
                            Box(modifier = Modifier.fillMaxSize().mouseBackButtonGoesBack(backInput)) { content() }
                        }
                    }
                }
            }
        }
    }
}
