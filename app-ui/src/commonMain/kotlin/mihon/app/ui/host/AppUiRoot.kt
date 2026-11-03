package mihon.app.ui.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import mihon.app.ui.di.AppUiGraph
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.platform.PlatformContext
import mihon.core.platform.currentPlatformContext
import mihon.core.platform.link.LinkOpener
import mihon.presentation.kit.content.ProvideTextRole
import mihon.presentation.kit.feedback.LocalAppMessages
import mihon.presentation.kit.surface.ContentOn
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalKatariThemeGraph
import mihon.presentation.kit.theme.TextRole

/**
 * What every composition of the app's screens is given, on every platform: [graph] and its ViewModels, the theme read
 * from it, links opened through [LinkOpener], whose failures show as a message instead of throwing as Compose's own
 * handler does, the app's messages, and the theme with the base surface and text role. The host provides around it
 * only what its platform alone has, such as a desktop window's platform context and density; the build fails when a
 * host provides one of these itself.
 */
@HostEntryPoint
@Composable
fun AppUiRoot(graph: AppUiGraph, content: @Composable () -> Unit) {
    val platformContext = currentPlatformContext()
    val uriHandler = remember(graph, platformContext) { LinkOpenerUriHandler(graph.linkOpener, platformContext) }
    CompositionLocalProvider(
        LocalAppUiGraph provides graph,
        LocalMetroViewModelFactory provides graph.viewModelFactory,
        LocalKatariThemeGraph provides graph,
        LocalUriHandler provides uriHandler,
        LocalAppMessages provides graph.appMessages,
    ) {
        KatariTheme {
            ContentOn(SurfaceContainer.Background) {
                ProvideTextRole(TextRole.Supporting, content)
            }
        }
    }
}

private class LinkOpenerUriHandler(
    private val linkOpener: LinkOpener,
    private val context: PlatformContext,
) : UriHandler {
    override fun openUri(uri: String) {
        linkOpener.open(context, uri)
    }
}
