package eu.kanade.tachiyomi.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mihon.app.ui.di.LocalAppUiGraph
import tachiyomi.presentation.core.util.collectAsState as collectPreferenceAsState

/** Which app state banners show over a window's screens. */
@Stable
class AppStateBannerState internal constructor(
    downloadedOnly: State<Boolean>,
    indexing: State<Boolean>,
    incognito: Boolean,
) {
    val downloadedOnly by downloadedOnly

    val indexing by indexing

    /** Set by [HomeNavigatorContent] for the screen showing, since a catalogue's extension can be incognito alone. */
    var incognito by mutableStateOf(incognito)
        internal set
}

@Composable
fun rememberAppStateBannerState(): AppStateBannerState {
    val graph = LocalAppUiGraph.current
    val downloadedOnly = graph.libraryPreferences.downloadedOnly.collectPreferenceAsState()
    val indexing = remember(graph) {
        graph.entryDownloadRuntimeFeature.state.map { it.isInitializing }.distinctUntilChanged()
    }.collectAsState(initial = false)
    return remember(graph) { AppStateBannerState(downloadedOnly, indexing, graph.getIncognitoState.await(null)) }
}
