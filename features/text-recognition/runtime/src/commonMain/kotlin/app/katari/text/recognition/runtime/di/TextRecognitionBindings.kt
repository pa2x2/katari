package app.katari.text.recognition.runtime.di

import app.katari.core.platform.storage.AppPrivateDirectories
import app.katari.model.artifacts.api.ModelArtifactStore
import app.katari.text.recognition.api.TextRecognitionFeature
import app.katari.text.recognition.api.host.TextRecognitionHostActions
import app.katari.text.recognition.runtime.cache.TextDetectionCache
import app.katari.text.recognition.runtime.cache.TextRecognitionResultCache
import app.katari.text.recognition.runtime.execution.CachedRecognitionExecutor
import app.katari.text.recognition.runtime.feature.DefaultTextRecognitionFeature
import app.katari.text.recognition.runtime.host.DefaultTextRecognitionHostActions
import app.katari.text.recognition.runtime.pipeline.PageTextDetection
import app.katari.text.recognition.runtime.registry.TextRecognitionComponentRegistry
import app.katari.text.recognition.runtime.selection.ProfileTextRecognitionPreferences
import app.katari.text.recognition.runtime.selection.TextRecognitionPipelineResolver
import app.katari.text.recognition.spi.contribution.TextRecognitionProviderContribution
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import java.io.File

@ContributesTo(AppScope::class)
interface TextRecognitionProviderMultibindings {
    /** Providers come from provider modules, and a build may include none. */
    @Multibinds(allowEmpty = true)
    fun providers(): Set<TextRecognitionProviderContribution>
}

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
        directories: AppPrivateDirectories,
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
                directory = { File(directories.cache, CACHE_DIRECTORY) },
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
}

private const val CACHE_DIRECTORY = "text-recognition"

/** Recognized regions are a few kilobytes per page, so this keeps thousands of pages. */
private const val CACHE_MAXIMUM_BYTES = 16L * 1024 * 1024

/** Detections are a few dozen rectangles per page; this covers the pages around the one being read. */
private const val DETECTION_CACHE_PAGES = 32
