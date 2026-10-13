package eu.kanade.tachiyomi.ui.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.webview.WebViewScreenContent

class WebViewScreen(
    private val url: String,
    private val initialTitle: String? = null,
    private val sourceId: Long? = null,
    private val headers: Map<String, String> = emptyMap(),
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel = viewModel { WebViewViewModel(sourceId) }

        WebViewScreenContent(
            onNavigateUp = { navigator.pop() },
            initialTitle = initialTitle,
            url = url,
            headers = viewModel.headers + headers,
            defaultUserAgentProvider = viewModel::defaultUserAgentProvider,
            onUrlChange = { assistUrl = it },
            onShare = { viewModel.shareWebpage(context, it) },
            onOpenInBrowser = { viewModel.openInBrowser(context, it) },
            onClearCookies = viewModel::clearCookies,
        )
    }
}
