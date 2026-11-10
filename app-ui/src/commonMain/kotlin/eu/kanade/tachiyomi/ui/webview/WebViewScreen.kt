package eu.kanade.tachiyomi.ui.webview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.webview.WebViewScreenContent
import eu.kanade.tachiyomi.ui.home.navigation.FullWindowScreen

/**
 * The web view screen in the main window's navigation. It reads the platform's browser from the app graph, as a screen
 * keeps only what its navigation can save.
 */
class WebViewScreen(
    private val url: String,
    private val initialTitle: String? = null,
    private val sourceId: Long? = null,
    private val headers: Map<String, String> = emptyMap(),
) : Screen(), AssistContentScreen, FullWindowScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val graph = LocalAppUiGraph.current
        val browserView = remember { graph.browserView }

        WebViewScreenContent(
            browserView = browserView,
            setup = graph.webViewSetup,
            url = url,
            initialTitle = initialTitle,
            sourceId = sourceId,
            headers = headers,
            onNavigateUp = { navigator.pop() },
            onUrlChange = { assistUrl = it },
        )
    }
}
