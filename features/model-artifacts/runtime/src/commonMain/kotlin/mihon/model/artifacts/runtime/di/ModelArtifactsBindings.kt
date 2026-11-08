package mihon.model.artifacts.runtime.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mihon.core.platform.network.NetworkConnectivity
import mihon.core.platform.storage.AppPrivateDirectories
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.runtime.download.ModelArtifactFileDownloader
import mihon.model.artifacts.runtime.network.ConnectivityModelArtifactNetworkPolicy
import mihon.model.artifacts.runtime.network.forModelArtifacts
import mihon.model.artifacts.runtime.storage.ModelArtifactStorage
import mihon.model.artifacts.runtime.store.DefaultModelArtifactStore
import java.io.File

@ContributesTo(AppScope::class)
@BindingContainer
object ModelArtifactsBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun store(
        directories: AppPrivateDirectories,
        connectivity: NetworkConnectivity,
        networkHelper: Lazy<NetworkHelper>,
    ): ModelArtifactStore {
        // Built on the first download: the app's client is still being created off the startup path.
        val httpClient by lazy { networkHelper.value.client.forModelArtifacts() }
        return DefaultModelArtifactStore(
            storage = ModelArtifactStorage { File(directories.noBackupFiles, STORAGE_DIRECTORY) },
            downloader = ModelArtifactFileDownloader { httpClient },
            networkPolicy = ConnectivityModelArtifactNetworkPolicy(connectivity),
            scope = CoroutineScope(SupervisorJob()),
            ioDispatcher = Dispatchers.IO,
        )
    }
}

private const val STORAGE_DIRECTORY = "model-artifacts"
