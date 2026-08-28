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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.SettingsAppearanceScreen
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.presentation.more.settings.screen.SettingsMainScreen
import eu.kanade.presentation.more.settings.screen.SettingsTrackingScreen
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.presentation.more.settings.screen.resolveSettingsStartScreen
import eu.kanade.presentation.util.DefaultNavigatorScreenTransition
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import mihon.entry.interactions.media.EntryViewerSettingsFeature
import mihon.entry.viewer.settings.ViewerSettingsCategory
import tachiyomi.presentation.core.components.TwoPanelBox
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

class SettingsScreen(
    private val destination: Int? = null,
    private val viewerSettingsSurfaceId: String? = null,
) : Screen() {

    constructor(destination: Destination) : this(destination.id)
    constructor(destination: Destination, viewerSettingsSurfaceId: String?) :
        this(destination.id, viewerSettingsSurfaceId)

    @Composable
    override fun Content() {
        val parentNavigator = LocalNavigator.currentOrThrow
        val activity = LocalActivity.current
        val close: () -> Unit = {
            if (!parentNavigator.pop()) {
                activity?.finish()
            }
        }
        val twoPane = isTabletUi()
        val viewerSettingsScreen = remember(destination, viewerSettingsSurfaceId) {
            if (destination != Destination.Readers.id || viewerSettingsSurfaceId == null) {
                null
            } else {
                Injekt.get<EntryViewerSettingsFeature>()
                    .destinations
                    .firstOrNull {
                        it.category == ViewerSettingsCategory.READER &&
                            it.surfaceId == viewerSettingsSurfaceId
                    }
                    ?.projection as? VoyagerScreen
            }
        }
        val initialScreen = when (destination) {
            Destination.About.id -> Destination.About
            Destination.DataAndStorage.id -> Destination.DataAndStorage
            Destination.Tracking.id -> Destination.Tracking
            Destination.Translation.id -> Destination.Translation
            Destination.Readers.id -> Destination.Readers
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
                TwoPanelBox(
                    modifier = Modifier
                        .windowInsetsPadding(insets)
                        .consumeWindowInsets(insets),
                    startContent = {
                        CompositionLocalProvider(LocalBackPress provides close) {
                            SettingsMainScreen.Content(twoPane = true)
                        }
                    },
                    endContent = { DefaultNavigatorScreenTransition(navigator = it) },
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
    }
}
