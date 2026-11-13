package app.katari.ui.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import app.katari.core.platform.PlatformContext
import app.katari.core.platform.currentPlatformContext
import app.katari.core.platform.link.LinkOpener
import app.katari.presentation.kit.content.ProvideTextRole
import app.katari.presentation.kit.feedback.LocalAppMessages
import app.katari.presentation.kit.surface.ContentOn
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.LocalKatariThemeGraph
import app.katari.presentation.kit.theme.TextRole
import app.katari.sync.ui.choice.ProvideSettingSyncMarks
import app.katari.ui.di.AppUiGraph
import app.katari.ui.di.LocalAppUiGraph
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

/**
 * What every composition of the app's screens is given, on every platform: [graph] and its ViewModels, the theme read
 * from it, links opened through [LinkOpener], whose failures show as a message instead of throwing as Compose's own
 * handler does, the app's messages, the theme with the base surface and text role, and the sync marks of settings rows.
 * The host provides around it only what its platform alone has, such as a desktop window's platform context and
 * density; the build fails when a host provides one of these itself.
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
                ProvideTextRole(TextRole.Supporting) { ProvideSettingSyncMarks(content) }
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
