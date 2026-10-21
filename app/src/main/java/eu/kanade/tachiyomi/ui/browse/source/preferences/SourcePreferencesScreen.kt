package eu.kanade.tachiyomi.ui.browse.source.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.presentation.more.settings.PreferenceScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.visualName
import mihon.app.di.appGraph
import mihon.entry.interactions.source.EntrySourceSettingsResolution
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold

class SourcePreferencesScreen(val sourceId: Long) : Screen() {

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val sourceManager = remember { context.appGraph.sourceManager }
        val host = remember(sourceId) {
            when (val settings = context.appGraph.entrySourceSettingsFeature.resolve(sourceId)) {
                is EntrySourceSettingsResolution.Available -> SourcePreferenceHost(context, settings)
                else -> null
            }
        }
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
                val revision = host.revision
                remember(host, revision) { host.settingsItems(host.sections()) }
            } else {
                emptyList()
            }
            PreferenceScreen(items = items, contentPadding = contentPadding)
        }
    }
}
