package mihon.text.recognition.runtime.di

import kotlinx.coroutines.Dispatchers
import mihon.feature.runtime.application.ApplicationFeatureRuntimeArtifacts
import mihon.feature.runtime.application.ApplicationFeatureRuntimeGraphValidator
import mihon.feature.runtime.application.ApplicationFeatureRuntimeModule
import mihon.feature.runtime.application.applicationFeatureRuntimeBoundary
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.runtime.cache.TextRecognitionResultCache
import mihon.text.recognition.runtime.component.TextRecognitionRuntimeContribution
import mihon.text.recognition.runtime.component.createTextRecognitionRuntimeContributions
import mihon.text.recognition.runtime.execution.CachedRecognitionExecutor
import mihon.text.recognition.runtime.feature.DefaultTextRecognitionFeature
import mihon.text.recognition.runtime.graph.TextRecognitionFeatureCapability
import mihon.text.recognition.runtime.graph.TextRecognitionFeatureContributor
import mihon.text.recognition.runtime.graph.TextRecognitionFeatureGraphStateValidator
import mihon.text.recognition.runtime.host.DefaultTextRecognitionHostActions
import mihon.text.recognition.runtime.registry.TextRecognitionComponentRegistry
import mihon.text.recognition.runtime.selection.ProfileTextRecognitionPreferences
import mihon.text.recognition.runtime.selection.TextRecognitionPipelineResolver
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get
import java.io.File

val textRecognitionFeatureRuntimeModule = ApplicationFeatureRuntimeModule(
    id = "text-recognition",
    contributor = TextRecognitionFeatureContributor,
    requiredModules = setOf("model-artifacts"),
) { context ->
    val contributions = createTextRecognitionRuntimeContributions(context.application, context.components)
    val registry = TextRecognitionComponentRegistry(
        contributions = contributions.flatMap(TextRecognitionRuntimeContribution::components),
        presetContributions = contributions.flatMap(TextRecognitionRuntimeContribution::presets),
    )
    val preferences = context.dependencies.profilePreferenceOwners.register(
        id = ProfilePreferenceOwnerId("text-recognition"),
        keyPatterns = setOf(ProfileTextRecognitionPreferences.SELECTION_KEY_FAMILY),
        factory = ::ProfileTextRecognitionPreferences,
    ).create()
    val resolver = TextRecognitionPipelineResolver(registry, preferences)
    val feature = DefaultTextRecognitionFeature(
        registry = registry,
        resolver = resolver,
        modelStore = get<ModelArtifactStore>(),
        executor = CachedRecognitionExecutor(
            cache = TextRecognitionResultCache(
                directory = { File(context.application.cacheDir, CACHE_DIRECTORY) },
                maximumBytes = CACHE_MAXIMUM_BYTES,
            ),
            ioDispatcher = Dispatchers.IO,
            inferenceDispatcher = Dispatchers.Default,
        ),
    )
    val hostActions = DefaultTextRecognitionHostActions(registry, preferences, resolver)

    addSingletonFactory<TextRecognitionFeature> { feature }
    addSingletonFactory<TextRecognitionHostActions> { hostActions }

    ApplicationFeatureRuntimeArtifacts(
        capabilityProviders = listOf(TextRecognitionFeatureCapability.bind(feature)),
        runtimeBoundaries = listOf(
            applicationFeatureRuntimeBoundary<TextRecognitionFeature> { feature },
            applicationFeatureRuntimeBoundary<TextRecognitionHostActions> { hostActions },
        ),
        graphValidators = listOf(
            ApplicationFeatureRuntimeGraphValidator { evaluation ->
                TextRecognitionFeatureGraphStateValidator(evaluation).validate()
            },
        ),
    )
}

private const val CACHE_DIRECTORY = "text-recognition"

/** Recognized regions are a few kilobytes per page, so this keeps thousands of pages. */
private const val CACHE_MAXIMUM_BYTES = 16L * 1024 * 1024
