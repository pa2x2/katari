package mihon.desktop.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import mihon.app.ui.host.HomeHost
import mihon.app.ui.host.HomeLaunchState
import mihon.desktop.profile.StartupProfileGate
import mihon.feature.profiles.core.ProfileStartupSnapshot

/**
 * The window's content: the home behind the startup profile gate, with [startup] what the app loaded before the window
 * opened. Desktop has no shortcuts, notifications or links that open a screen, and the window shows from its first
 * frame, with no splash screen to lift.
 */
@Composable
fun HomeWindowContent(startup: ProfileStartupSnapshot) {
    HomeHost(
        launchState = remember { HomeLaunchState() },
        launchScope = rememberCoroutineScope(),
        onReady = {},
        profileGate = { _, home -> StartupProfileGate(startup, home) },
    )
}
