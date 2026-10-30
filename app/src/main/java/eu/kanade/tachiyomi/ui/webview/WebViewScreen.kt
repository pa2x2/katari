package eu.kanade.tachiyomi.ui.webview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.webview.WebViewScreenContent
import eu.kanade.tachiyomi.ui.home.navigation.FullWindowScreen
import mihon.app.di.appGraph

/**
 * The web view screen in the main window's navigation. It reads Android's browser from the app graph, as a screen keeps
 * only what its navigation can save.
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
        val context = LocalContext.current
        val browserView = remember { context.appGraph.browserView }

        WebViewScreenContent(
            browserView = browserView,
            url = url,
            initialTitle = initialTitle,
            sourceId = sourceId,
            headers = headers,
            onNavigateUp = { navigator.pop() },
            onUrlChange = { assistUrl = it },
        )
    }
}
