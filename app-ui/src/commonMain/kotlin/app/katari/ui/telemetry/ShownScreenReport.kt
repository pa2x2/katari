package app.katari.ui.telemetry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import app.katari.core.common.HomeScreenTabs
import app.katari.telemetry.Telemetry
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.home.HomeScreen

/**
 * Tells telemetry what [navigator] shows as it changes, so that crash reports say where they happened: the screen on
 * top, or for the home screen its [homeTab], the type of the entry an [EntryTypeScreen] is about, and the window's
 * density.
 */
@Composable
fun ShownScreenReport(navigator: Navigator, homeTab: () -> HomeScreenTabs?) {
    val density = LocalDensity.current.density
    LaunchedEffect(navigator, density) {
        snapshotFlow {
            val screen = navigator.lastItem
            val name = (if (screen is HomeScreen) homeTab()?.name else null) ?: screen::class.simpleName.orEmpty()
            name to (screen as? EntryTypeScreen)?.entryType
        }.collect { (name, entryType) -> Telemetry.setShownScreen(name, entryType?.name?.lowercase(), density) }
    }
}
