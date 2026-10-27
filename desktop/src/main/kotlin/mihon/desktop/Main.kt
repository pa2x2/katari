package mihon.desktop

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import coil3.SingletonImageLoader
import dev.zacsweers.metro.createGraph
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import kotlinx.coroutines.runBlocking
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.platform.LocalPlatformContext
import mihon.desktop.di.DesktopGraph
import mihon.desktop.window.WindowPlatformContext
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.ScreenMessagesHost
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalKatariThemeGraph
import tachiyomi.i18n.MR
import tachiyomi.i18n.app_name
import tachiyomi.i18n.information_empty_library
import tachiyomi.i18n.label_library
import tachiyomi.presentation.core.i18n.stringResource

fun main() {
    val graph = createGraph<DesktopGraph>()
    SingletonImageLoader.setSafe { graph.imageLoader }
    // As Android's launch does before its first screen, which also offers the profile picker that desktop lacks yet:
    // the default profile exists, older preferences are moved and the profile last open is active again.
    runBlocking { graph.profileManager.loadStartupSnapshot() }

    application {
        Window(onCloseRequest = ::exitApplication, title = stringResource(MR.strings.app_name)) {
            val platformContext = remember(window) { WindowPlatformContext(window) }
            CompositionLocalProvider(
                LocalAppUiGraph provides graph,
                LocalKatariThemeGraph provides graph,
                LocalMetroViewModelFactory provides graph.viewModelFactory,
                LocalPlatformContext provides platformContext,
            ) {
                KatariTheme {
                    ScreenMessagesHost {
                        Scaffold(
                            topBar = { scrollBehavior ->
                                AppBar(
                                    title = stringResource(MR.strings.label_library),
                                    scrollBehavior = scrollBehavior,
                                )
                            },
                        ) { contentPadding ->
                            EmptyScreen(MR.strings.information_empty_library, Modifier.padding(contentPadding))
                        }
                    }
                }
            }
        }
    }
}
