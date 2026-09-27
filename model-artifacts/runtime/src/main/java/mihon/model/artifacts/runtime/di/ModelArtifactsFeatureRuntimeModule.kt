package mihon.model.artifacts.runtime.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mihon.feature.runtime.application.ApplicationFeatureRuntimeArtifacts
import mihon.feature.runtime.application.ApplicationFeatureRuntimeGraphValidator
import mihon.feature.runtime.application.ApplicationFeatureRuntimeModule
import mihon.feature.runtime.application.applicationFeatureRuntimeBoundary
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.runtime.download.ModelArtifactFileDownloader
import mihon.model.artifacts.runtime.graph.ModelArtifactStoreCapability
import mihon.model.artifacts.runtime.graph.ModelArtifactsFeatureContributor
import mihon.model.artifacts.runtime.graph.ModelArtifactsFeatureGraphStateValidator
import mihon.model.artifacts.runtime.network.AndroidModelArtifactNetworkPolicy
import mihon.model.artifacts.runtime.network.ModelArtifactNetwork
import mihon.model.artifacts.runtime.storage.ModelArtifactStorage
import mihon.model.artifacts.runtime.store.DefaultModelArtifactStore
import uy.kohesive.injekt.api.addSingletonFactory
import java.io.File

val modelArtifactsFeatureRuntimeModule = ApplicationFeatureRuntimeModule(
    id = "model-artifacts",
    contributor = ModelArtifactsFeatureContributor,
) { context ->
    val store = DefaultModelArtifactStore(
        storage = ModelArtifactStorage { File(context.application.noBackupFilesDir, STORAGE_DIRECTORY) },
        downloader = ModelArtifactFileDownloader(ModelArtifactNetwork.httpClient),
        networkPolicy = AndroidModelArtifactNetworkPolicy(context.application),
        scope = CoroutineScope(SupervisorJob()),
        ioDispatcher = Dispatchers.IO,
    )

    addSingletonFactory<ModelArtifactStore> { store }

    ApplicationFeatureRuntimeArtifacts(
        capabilityProviders = listOf(ModelArtifactStoreCapability.bind(store)),
        runtimeBoundaries = listOf(applicationFeatureRuntimeBoundary<ModelArtifactStore> { store }),
        graphValidators = listOf(
            ApplicationFeatureRuntimeGraphValidator { evaluation ->
                ModelArtifactsFeatureGraphStateValidator(evaluation).validate()
            },
        ),
    )
}

private const val STORAGE_DIRECTORY = "model-artifacts"
