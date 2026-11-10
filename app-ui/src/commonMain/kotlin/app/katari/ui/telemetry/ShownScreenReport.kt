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
 * top, or for the home screen its [homeTab], the type of the entry it is about from [entryTypes], and the window's
 * density.
 */
@Composable
fun ShownScreenReport(navigator: Navigator, entryTypes: ShownEntryTypes, homeTab: () -> HomeScreenTabs?) {
    val density = LocalDensity.current.density
    LaunchedEffect(navigator, entryTypes, density) {
        snapshotFlow {
            val screen = navigator.lastItem
            val name = (if (screen is HomeScreen) homeTab()?.name else null) ?: screen::class.simpleName.orEmpty()
            name to entryTypes[screen]
        }.collect { (name, entryType) -> Telemetry.setShownScreen(name, entryType?.name?.lowercase(), density) }
    }
}
