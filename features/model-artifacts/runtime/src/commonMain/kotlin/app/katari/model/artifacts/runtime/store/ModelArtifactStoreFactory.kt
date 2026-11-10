package app.katari.model.artifacts.runtime.store

import app.katari.core.platform.network.NetworkConnectivity
import app.katari.model.artifacts.api.ModelArtifactStore
import app.katari.model.artifacts.runtime.download.ModelArtifactFileDownloader
import app.katari.model.artifacts.runtime.network.ConnectivityModelArtifactNetworkPolicy
import app.katari.model.artifacts.runtime.network.forModelArtifacts
import app.katari.model.artifacts.runtime.storage.ModelArtifactStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.io.File

/**
 * A store that keeps its artifacts in [directory] and downloads them with [httpClient], both asked for on first use.
 * The app's models share one store; a download that isn't a model and must not show among them, or be removed with
 * them, gets a store of its own in a folder of its own.
 */
fun modelArtifactStore(
    directory: () -> File,
    connectivity: NetworkConnectivity,
    httpClient: () -> OkHttpClient,
): ModelArtifactStore {
    val artifactClient by lazy { httpClient().forModelArtifacts() }
    return DefaultModelArtifactStore(
        storage = ModelArtifactStorage(directory),
        downloader = ModelArtifactFileDownloader { artifactClient },
        networkPolicy = ConnectivityModelArtifactNetworkPolicy(connectivity),
        scope = CoroutineScope(SupervisorJob()),
        ioDispatcher = Dispatchers.IO,
    )
}
