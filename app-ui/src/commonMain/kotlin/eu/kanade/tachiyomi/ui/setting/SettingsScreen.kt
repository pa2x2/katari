package eu.kanade.tachiyomi.ui.setting

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.screen.SettingsMainScreen
import eu.kanade.presentation.more.settings.screen.resolveSettingsStartScreen
import eu.kanade.presentation.util.DefaultNavigatorScreenTransition
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.rememberScreenHostFinish
import mihon.app.ui.di.LocalAppUiGraph
import mihon.presentation.kit.layout.TwoPanes
import mihon.presentation.kit.window.currentWindowLayout
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

class SettingsScreen(
    private val destination: Int? = null,
    private val viewerSettingsSurfaceId: String? = null,
    /** The title of the viewer settings row to scroll to and pulse. */
    private val viewerSettingsHighlight: String? = null,
) : Screen() {

    constructor(destination: Destination) : this(destination.id)
    constructor(destination: Destination, viewerSettingsSurfaceId: String?, viewerSettingsHighlight: String?) :
        this(destination.id, viewerSettingsSurfaceId, viewerSettingsHighlight)

    @Composable
    override fun Content() {
        val parentNavigator = LocalNavigator.currentOrThrow
        val finishHost = rememberScreenHostFinish()
        val viewerSettingsFeature = LocalAppUiGraph.current.entryViewerSettingsFeature
        val close: () -> Unit = {
            if (!parentNavigator.pop()) {
                finishHost()
            }
        }
        val twoPane = currentWindowLayout().usesTwoPanes
        val viewerSettingsScreen = remember(destination, viewerSettingsSurfaceId) {
            if (destination != Destination.Readers.id || viewerSettingsSurfaceId == null) {
                null
            } else {
                val screen = viewerSettingsFeature
                    .destinations
                    .firstOrNull { it.surfaceId == viewerSettingsSurfaceId }
                    ?.projection as? VoyagerScreen
                // Set like a settings search result: before the screen's list first composes, which clears it.
                if (screen != null && viewerSettingsHighlight != null) {
                    SearchableSettings.highlightKey = viewerSettingsHighlight
                }
                screen
            }
        }
        val initialScreen = when (destination) {
            Destination.About.id -> Destination.About
            Destination.DataAndStorage.id -> Destination.DataAndStorage
            Destination.Tracking.id -> Destination.Tracking
            Destination.Translation.id -> Destination.Translation
            Destination.Readers.id -> Destination.Readers
            Destination.TextRecognition.id -> Destination.TextRecognition
            Destination.LibraryUpdates.id -> Destination.LibraryUpdates
            Destination.Profiles.id -> Destination.Profiles
            Destination.Sync.id -> Destination.Sync
            else -> null
        }.let {
            resolveSettingsStartScreen(
                destination = it,
                twoPane = twoPane,
                viewerSettingsScreen = viewerSettingsScreen,
            )
        }

        if (!twoPane) {
            Navigator(
                screen = initialScreen,
                onBackPressed = null,
            ) {
                val pop: () -> Unit = {
                    if (it.canPop) {
                        it.pop()
                    } else {
                        close()
                    }
                }
                CompositionLocalProvider(LocalBackPress provides pop) {
                    DefaultNavigatorScreenTransition(navigator = it)
                }
            }
        } else {
            Navigator(
                screen = initialScreen,
                onBackPressed = null,
            ) { navigator ->
                val insets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                TwoPanes(
                    modifier = Modifier
                        .windowInsetsPadding(insets)
                        .consumeWindowInsets(insets),
                    start = {
                        CompositionLocalProvider(LocalBackPress provides close) {
                            SettingsMainScreen.Content(twoPane = true)
                        }
                    },
                    end = { DefaultNavigatorScreenTransition(navigator = navigator) },
                )
            }
        }
    }

    sealed class Destination(val id: Int) {
        data object About : Destination(0)
        data object DataAndStorage : Destination(1)
        data object Tracking : Destination(2)
        data object Translation : Destination(3)
        data object Readers : Destination(4)
        data object TextRecognition : Destination(5)
        data object LibraryUpdates : Destination(6)
        data object Profiles : Destination(7)
        data object Sync : Destination(8)
    }
}
