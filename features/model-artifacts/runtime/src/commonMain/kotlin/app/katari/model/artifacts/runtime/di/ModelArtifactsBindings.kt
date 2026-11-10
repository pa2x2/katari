package app.katari.model.artifacts.runtime.di

import app.katari.core.platform.network.NetworkConnectivity
import app.katari.core.platform.storage.AppPrivateDirectories
import app.katari.model.artifacts.api.ModelArtifactStore
import app.katari.model.artifacts.runtime.store.modelArtifactStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
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
    ): ModelArtifactStore = modelArtifactStore(
        directory = { File(directories.noBackupFiles, STORAGE_DIRECTORY) },
        connectivity = connectivity,
        // Built on the first download: the app's client is still being created off the startup path
        httpClient = { networkHelper.value.client },
    )
}

private const val STORAGE_DIRECTORY = "model-artifacts"
