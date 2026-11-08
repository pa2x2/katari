package eu.kanade.tachiyomi.ui.browse.source.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.presentation.more.settings.PreferenceScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.visualName

class SourcePreferencesScreen(val sourceId: Long) : Screen() {

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val context = currentPlatformContext()
        val navigator = LocalNavigator.currentOrThrow
        val graph = LocalAppUiGraph.current
        val sourceManager = remember { graph.sourceManager }
        val host = remember(sourceId) { graph.sourcePreferenceHosts.open(sourceId, context) }
        DisposableEffect(host) {
            onDispose { host?.release() }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = sourceManager.getDisplayInfo(sourceId).visualName(),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            val items = if (host != null) {
                val revision by host.revision.collectAsState()
                remember(host, revision) { sourcePreferenceItems(host.sections()) }
            } else {
                emptyList()
            }
            PreferenceScreen(items = items, contentPadding = contentPadding)
        }
    }
}
