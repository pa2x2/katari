package eu.kanade.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import app.katari.ui.di.LocalAppUiGraph

@Composable
fun ifSourcesLoaded(): Boolean {
    val graph = LocalAppUiGraph.current
    return remember { graph.sourceManager.isInitialized }.collectAsState().value
}

@Composable
fun ifCatalogSourcesLoaded(): Boolean {
    val graph = LocalAppUiGraph.current
    return remember { graph.entryCatalogueFeature.isInitialized }.collectAsState().value
}
