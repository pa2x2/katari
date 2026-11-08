package mihon.desktop.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import cafe.adriel.voyager.core.lifecycle.LocalNavigatorScreenLifecycleProvider
import eu.kanade.tachiyomi.ui.base.lifecycle.DesktopScreenLifecycleOwner
import kotlinx.coroutines.flow.Flow
import mihon.app.ui.host.AppUiRoot
import mihon.core.platform.LocalPlatformContext
import mihon.desktop.application.DesktopAppVariant
import mihon.desktop.application.icon.AppIcon
import mihon.desktop.application.instance.InstanceRequest
import mihon.desktop.di.DesktopGraph
import mihon.desktop.display.size.InterfaceSizeShortcuts
import mihon.desktop.display.size.ProvideInterfaceDensity
import mihon.desktop.extension.dialog.ExtensionDialogsHost
import mihon.desktop.presence.ReportsPresence
import mihon.desktop.security.signin.SignInPromptHost
import mihon.desktop.work.WorkCloseGuardDialog
import mihon.presentation.kit.feedback.AppMessagesHost
import mihon.presentation.kit.files.LocalSystemFileChooser
import mihon.presentation.kit.pointer.LocalAppPointerPresence
import mihon.presentation.kit.shortcut.KeyboardShortcutRegistry
import mihon.presentation.kit.shortcut.KeyboardShortcutsHost
import mihon.presentation.kit.window.LocalScreenTitles
import mihon.presentation.kit.window.LocalWindowFullScreen
import mihon.presentation.kit.window.ScreenTitles
import tachiyomi.i18n.MR
import tachiyomi.i18n.window_title_with_screen
import tachiyomi.presentation.core.i18n.stringResource
import javax.swing.JFrame

/**
 * The app's window: framed by the desktop, back where it was last time, titled with the app's name and the open
 * screen's title unless the profile is locked, carrying the app's icon, brought forward when the app is launched again
 * and opening what that launch opens, and closing the app when it closes, after asking while background work runs (see
 * [mihon.desktop.work.WorkCloseGuard]). [content] gets what [AppUiRoot] gives every platform's screens, and around it what only a
 * desktop window has: the window as platform context, a lifecycle and ViewModel store for each screen its navigators
 * show, as Voyager gives them on Android, the window's keyboard shortcuts, which get the keys the content leaves, the
 * window's screen titles, which the screens' app bars report to, the window's full screen, which F11 and screens such
 * as the player enter, whether the mouse is over the app, which the player's controls leave with, the desktop's file
 * chooser, which the screens' document pickers open, the app's messages, which show on the screen the user is on, the
 * profile picker included, and the density of the session's scale and the interface size. While it is open,
 * extensions find the activity that stands for it in the foreground
 * ([mihon.extension.runtime.jvm.application.WindowActivity]), and the dialogs they show over that activity open in it.
 * Its focus is the app's foreground for sync ([ReportsForeground]), and whether it is minimized counts toward the user
 * being away, which locks the open profile again ([ReportsPresence]).
 */
@Composable
fun ApplicationScope.KatariWindow(
    graph: DesktopGraph,
    laterLaunches: Flow<InstanceRequest>,
    content: @Composable () -> Unit,
) {
    val state = rememberSavedWindowState(graph.windowPreferences)
    val opening = remember(state) { OpeningBounds(state) }
    val shortcuts = remember { KeyboardShortcutRegistry() }
    val screenTitles = remember { ScreenTitles() }
    val appName = DesktopAppVariant.current.displayName
    // A locked profile's screen stays out of the title, which the taskbar and window switcher show
    val profileLocked by graph.activeProfileLock.isHeld.collectAsState()
    val screenTitle = screenTitles.current.takeUnless { profileLocked }
    val title = screenTitle?.let { stringResource(MR.strings.window_title_with_screen, appName, it) } ?: appName
    val icon = remember { AppIcon.painter() }
    val requestQuit = { graph.workCloseGuard.requestQuit(::exitApplication) }
    Window(
        onCloseRequest = requestQuit,
        state = state,
        title = title,
        icon = icon,
        onKeyEvent = shortcuts::onKeyEvent,
    ) {
        OpensAtBounds(window, state, opening)
        LeavesFullScreenToMaximized(window, state)
        WindowContent(
            graph = graph,
            window = window,
            state = state,
            shortcuts = shortcuts,
            screenTitles = screenTitles,
            laterLaunches = laterLaunches,
            onQuit = requestQuit,
            onConfirmedQuit = ::exitApplication,
            content = content,
        )
    }
}

@Composable
private fun WindowContent(
    graph: DesktopGraph,
    window: JFrame,
    state: WindowState,
    shortcuts: KeyboardShortcutRegistry,
    screenTitles: ScreenTitles,
    laterLaunches: Flow<InstanceRequest>,
    onQuit: () -> Unit,
    onConfirmedQuit: () -> Unit,
    content: @Composable () -> Unit,
) {
    val platformContext = remember(window) { WindowPlatformContext(window) }
    val fullScreen = remember(state) { PlacementFullScreen(state) }
    val pointerPresence = remember { AwtAppPointerPresence() }
    DisposableEffect(pointerPresence) {
        onDispose(pointerPresence::close)
    }
    DisposableEffect(platformContext) {
        graph.openWindow.context = platformContext
        onDispose {
            if (graph.openWindow.context === platformContext) graph.openWindow.context = null
        }
    }
    DisposableEffect(graph.windowActivity) {
        graph.windowActivity.onWindowOpened()
        onDispose { graph.windowActivity.onWindowClosed() }
    }
    ReportsForeground(window, graph)
    ReportsPresence(window, graph.windowAnimePlayer, graph.appPresence)
    AnswerLaterLaunches(window, laterLaunches, graph.displayBackend, graph.homeDestinations)
    CompositionLocalProvider(
        LocalNavigatorScreenLifecycleProvider provides DesktopScreenLifecycleOwner,
        LocalPlatformContext provides platformContext,
        LocalScreenTitles provides screenTitles,
        LocalWindowFullScreen provides fullScreen,
        LocalAppPointerPresence provides pointerPresence,
        LocalSystemFileChooser provides graph.systemFileChooser,
    ) {
        ProvideInterfaceDensity(graph.sessionScale, graph.interfaceSizePreferences) {
            AppUiRoot(graph) {
                // Around the profile gate too, so a message reaches its picker before a profile opens
                AppMessagesHost(graph.appMessages) {
                    KeyboardShortcutsHost(shortcuts) {
                        val backInput = rememberWindowBackInput()
                        WindowShortcuts(fullScreen = fullScreen, backInput = backInput, onQuit = onQuit)
                        InterfaceSizeShortcuts(graph.interfaceSizePreferences)
                        Box(modifier = Modifier.fillMaxSize().mouseBackButtonGoesBack(shortcuts, backInput)) {
                            content()
                        }
                        ExtensionDialogsHost(graph.windowExtensionDialogs)
                        SignInPromptHost(graph.signInPrompts, graph.systemSignIn)
                        WorkCloseGuardDialog(graph.workCloseGuard, quit = onConfirmedQuit)
                    }
                }
            }
        }
    }
}
