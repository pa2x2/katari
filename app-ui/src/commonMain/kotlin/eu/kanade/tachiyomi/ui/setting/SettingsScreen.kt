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
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.layout.TwoPanes
import app.katari.presentation.kit.window.currentWindowLayout
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.AppEntryViewerSettingsScreenProjection
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.screen.SettingsMainList
import eu.kanade.presentation.more.settings.screen.resolveSettingsStartScreen
import eu.kanade.presentation.util.DefaultNavigatorScreenTransition
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.rememberScreenHostFinish

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
                val screen = (
                    viewerSettingsFeature
                        .destinations
                        .firstOrNull { it.surfaceId == viewerSettingsSurfaceId }
                        ?.projection as? AppEntryViewerSettingsScreenProjection
                    )
                    ?.newScreen()
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

        // One navigator for both layouts, so crossing between them keeps the open page and what it shows (dialogs
        // included): its screen area moves between the layouts whole, and its stack takes the other layout's shape.
        Navigator(
            screen = initialScreen,
            onBackPressed = null,
        ) { navigator ->
            // The layout the stack is shaped for, saved along with the stack
            var stackTwoPane by rememberSaveable { mutableStateOf(twoPane) }
            if (stackTwoPane != twoPane) {
                navigator.replaceAll(settingsStackFor(twoPane, navigator.items))
                stackTwoPane = twoPane
            }
            val screens = remember(navigator) { movableContentOf { DefaultNavigatorScreenTransition(navigator) } }
            if (!twoPane) {
                val pop: () -> Unit = {
                    if (navigator.canPop) {
                        navigator.pop()
                    } else {
                        close()
                    }
                }
                CompositionLocalProvider(LocalBackPress provides pop) { screens() }
            } else {
                val insets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                TwoPanes(
                    modifier = Modifier
                        .windowInsetsPadding(insets)
                        .consumeWindowInsets(insets),
                    start = {
                        CompositionLocalProvider(LocalBackPress provides close) {
                            SettingsMainList(twoPane = true)
                        }
                    },
                    end = { screens() },
                )
            }
        }
    }

    sealed class Destination(val id: Int) {
        data object About : Destination(0)
        data object DataAndStorage : Destination(1)
        data object Translation : Destination(3)
        data object Readers : Destination(4)
        data object TextRecognition : Destination(5)
        data object LibraryUpdates : Destination(6)
        data object Profiles : Destination(7)
        data object Sync : Destination(8)
    }
}
