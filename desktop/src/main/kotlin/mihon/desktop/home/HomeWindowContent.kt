package mihon.desktop.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import cafe.adriel.voyager.navigator.Navigator
import mihon.app.ui.host.HomeHost
import mihon.app.ui.host.HomeLaunchState
import mihon.core.platform.currentPlatformContext
import mihon.desktop.profile.ActiveProfileLock
import mihon.desktop.profile.WindowProfileGate
import mihon.desktop.window.WindowPlatformContext
import mihon.feature.profiles.core.ProfileStartupSnapshot

/**
 * The window's content: the home behind the window's profile gate, which [profileLock] holds while the open profile is
 * locked, with [startup] what the app loaded before the window opened. The window shows from its first frame, with no splash screen to lift. The home's navigator becomes the
 * window's, for screens opened from outside one, and opens what [destinations] kept waiting for it, such as a
 * notification's destination in the profile the home has just switched to. [navigatorContent] goes over the home's
 * screens, such as prompts and the player.
 */
@Composable
fun HomeWindowContent(
    startup: ProfileStartupSnapshot,
    profileLock: ActiveProfileLock,
    destinations: HomeDestinations,
    navigatorContent: @Composable (Navigator) -> Unit,
) {
    val window = currentPlatformContext() as? WindowPlatformContext
    HomeHost(
        launchState = remember { HomeLaunchState() },
        launchScope = rememberCoroutineScope(),
        onReady = {},
        profileGate = { _, home -> WindowProfileGate(startup, profileLock, home) },
        onNavigator = { navigator, _ ->
            window?.navigator = navigator
            destinations.homeStarted(navigator)
        },
        navigatorContent = navigatorContent,
    )
}
