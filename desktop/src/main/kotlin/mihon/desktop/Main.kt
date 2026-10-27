package mihon.desktop

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import eu.kanade.domain.ui.UiPreferences
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.ScreenMessagesHost
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.KatariThemeGraph
import mihon.presentation.kit.theme.LocalKatariThemeGraph
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.i18n.MR
import tachiyomi.i18n.app_name
import tachiyomi.i18n.information_empty_library
import tachiyomi.i18n.label_library
import tachiyomi.presentation.core.i18n.stringResource

fun main() {
    // Nothing on desktop changes settings yet; a store on disk comes with the first screen that does.
    val themeGraph = object : KatariThemeGraph {
        override val uiPreferences = UiPreferences(InMemoryPreferenceStore())
    }

    application {
        Window(onCloseRequest = ::exitApplication, title = stringResource(MR.strings.app_name)) {
            CompositionLocalProvider(LocalKatariThemeGraph provides themeGraph) {
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
