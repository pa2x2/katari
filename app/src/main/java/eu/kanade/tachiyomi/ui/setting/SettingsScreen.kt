package eu.kanade.tachiyomi.ui.setting

import androidx.activity.compose.LocalActivity
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
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.screen.SettingsAppearanceScreen
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.presentation.more.settings.screen.SettingsMainScreen
import eu.kanade.presentation.more.settings.screen.SettingsTrackingScreen
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.presentation.more.settings.screen.resolveSettingsStartScreen
import eu.kanade.presentation.util.DefaultNavigatorScreenTransition
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import mihon.app.di.appGraph
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
        val activity = LocalActivity.current
        val context = LocalContext.current
        val close: () -> Unit = {
            if (!parentNavigator.pop()) {
                activity?.finish()
            }
        }
        val twoPane = currentWindowLayout().usesTwoPanes
        val viewerSettingsScreen = remember(destination, viewerSettingsSurfaceId) {
            if (destination != Destination.Readers.id || viewerSettingsSurfaceId == null) {
                null
            } else {
                val screen = context.appGraph.entryViewerSettingsFeature
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
            ) {
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
                    end = { DefaultNavigatorScreenTransition(navigator = it) },
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
    }
}
