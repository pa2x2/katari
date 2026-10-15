package mihon.text.recognition.runtime.di

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.application.ApplicationFeatureGraphValidator
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.runtime.cache.TextDetectionCache
import mihon.text.recognition.runtime.cache.TextRecognitionResultCache
import mihon.text.recognition.runtime.execution.CachedRecognitionExecutor
import mihon.text.recognition.runtime.feature.DefaultTextRecognitionFeature
import mihon.text.recognition.runtime.graph.TextRecognitionFeatureCapability
import mihon.text.recognition.runtime.graph.TextRecognitionFeatureContributor
import mihon.text.recognition.runtime.graph.TextRecognitionFeatureGraphStateValidator
import mihon.text.recognition.runtime.host.DefaultTextRecognitionHostActions
import mihon.text.recognition.runtime.pipeline.PageTextDetection
import mihon.text.recognition.runtime.registry.TextRecognitionComponentRegistry
import mihon.text.recognition.runtime.selection.ProfileTextRecognitionPreferences
import mihon.text.recognition.runtime.selection.TextRecognitionPipelineResolver
import mihon.text.recognition.spi.contribution.TextRecognitionProviderContribution
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import java.io.File

@ContributesTo(AppScope::class)
@BindingContainer
object TextRecognitionBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun registry(providers: Set<TextRecognitionProviderContribution>): TextRecognitionComponentRegistry =
        TextRecognitionComponentRegistry(providers.toList())

    @Provides
    @SingleIn(AppScope::class)
    internal fun preferencesOwner(
        owners: ProfilePreferenceOwnerInstaller,
    ): InstalledProfilePreferenceOwner<ProfileTextRecognitionPreferences> = owners.register(
        id = ProfilePreferenceOwnerId("text-recognition"),
        keyPatterns = setOf(ProfileTextRecognitionPreferences.SELECTION_KEY_FAMILY),
        factory = ::ProfileTextRecognitionPreferences,
    )

    @Provides
    @IntoSet
    internal fun preferencesOwnerRegistration(
        owner: InstalledProfilePreferenceOwner<ProfileTextRecognitionPreferences>,
    ): InstalledProfilePreferenceOwner<*> = owner

    @Provides
    @SingleIn(AppScope::class)
    internal fun preferences(
        owner: InstalledProfilePreferenceOwner<ProfileTextRecognitionPreferences>,
    ): ProfileTextRecognitionPreferences = owner.create()

    @Provides
    @SingleIn(AppScope::class)
    internal fun resolver(registry: TextRecognitionComponentRegistry): TextRecognitionPipelineResolver =
        TextRecognitionPipelineResolver(registry)

    @Provides
    @SingleIn(AppScope::class)
    internal fun feature(
        application: Application,
        registry: TextRecognitionComponentRegistry,
        resolver: TextRecognitionPipelineResolver,
        preferences: ProfileTextRecognitionPreferences,
        modelStore: ModelArtifactStore,
    ): TextRecognitionFeature = DefaultTextRecognitionFeature(
        registry = registry,
        resolver = resolver,
        preferences = preferences,
        modelStore = modelStore,
        executor = CachedRecognitionExecutor(
            cache = TextRecognitionResultCache(
                directory = { File(application.cacheDir, CACHE_DIRECTORY) },
                maximumBytes = CACHE_MAXIMUM_BYTES,
            ),
            ioDispatcher = Dispatchers.IO,
            inferenceDispatcher = Dispatchers.Default,
        ),
        detection = PageTextDetection(TextDetectionCache(DETECTION_CACHE_PAGES)),
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun hostActions(
        registry: TextRecognitionComponentRegistry,
        preferences: ProfileTextRecognitionPreferences,
        resolver: TextRecognitionPipelineResolver,
    ): TextRecognitionHostActions = DefaultTextRecognitionHostActions(registry, preferences, resolver)

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = TextRecognitionFeatureContributor

    @Provides
    @IntoSet
    fun featureCapability(feature: TextRecognitionFeature): CapabilityProvider<*> =
        TextRecognitionFeatureCapability.bind(feature)

    @Provides
    @IntoSet
    fun graphValidator(): ApplicationFeatureGraphValidator = ApplicationFeatureGraphValidator { evaluation ->
        TextRecognitionFeatureGraphStateValidator(evaluation).validate()
    }
}

private const val CACHE_DIRECTORY = "text-recognition"

/** Recognized regions are a few kilobytes per page, so this keeps thousands of pages. */
private const val CACHE_MAXIMUM_BYTES = 16L * 1024 * 1024

/** Detections are a few dozen rectangles per page; this covers the pages around the one being read. */
private const val DETECTION_CACHE_PAGES = 32
