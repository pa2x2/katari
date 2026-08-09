package mihon.tts.runtime.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mihon.feature.runtime.application.ApplicationFeatureRuntimeArtifacts
import mihon.feature.runtime.application.ApplicationFeatureRuntimeGraphValidator
import mihon.feature.runtime.application.ApplicationFeatureRuntimeModule
import mihon.feature.runtime.application.applicationFeatureRuntimeBoundary
import mihon.language.runtime.identification.createPlatformTextLanguageDetectors
import mihon.tts.api.TtsFeature
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.host.TtsHostActions
import mihon.tts.runtime.audio.AndroidTtsAudioFocus
import mihon.tts.runtime.component.TtsRuntimeContribution
import mihon.tts.runtime.component.createTtsRuntimeContributions
import mihon.tts.runtime.feature.DefaultTtsFeature
import mihon.tts.runtime.graph.TtsEngineRegistryCapability
import mihon.tts.runtime.graph.TtsFeatureContributor
import mihon.tts.runtime.graph.TtsFeatureGraphStateValidator
import mihon.tts.runtime.host.DefaultTtsHostActions
import mihon.tts.runtime.preference.ProfileTtsPreferences
import mihon.tts.runtime.registry.DefaultTtsEngineRegistry
import mihon.tts.runtime.selection.ProfileTtsEngineResolver
import mihon.tts.spi.engine.KnownTtsEngineCatalog
import mihon.tts.spi.engine.TtsEngineRegistry
import mihon.tts.spi.setup.TtsEngineSetupRegistry
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import uy.kohesive.injekt.api.addSingletonFactory

val ttsFeatureRuntimeModule = ApplicationFeatureRuntimeModule(
    id = "tts",
    contributor = TtsFeatureContributor,
) { context ->
    val contributions = createTtsRuntimeContributions(
        application = context.application,
        components = context.components,
    ).flatMap(TtsRuntimeContribution::engineContributions)
    val registry = DefaultTtsEngineRegistry(contributions)
    val initialEngine = registry.engines.firstOrNull()?.catalogEntry?.id
    val profilePreferencesOwner = context.dependencies.profilePreferenceOwners.register(
        id = ProfilePreferenceOwnerId("tts"),
        keyPatterns = setOf(ProfileTtsPreferences.VOICE_KEY_FAMILY),
        factory = { store -> ProfileTtsPreferences(store, initialEngine) },
    )
    val preferences = profilePreferencesOwner.create()
    val engineResolver = ProfileTtsEngineResolver(preferences)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var feature: DefaultTtsFeature
    val audioFocus = AndroidTtsAudioFocus(context.application) {
        feature.onAudioFocusLost()
    }
    feature = DefaultTtsFeature(
        engineRegistry = registry,
        knownEngineCatalog = registry,
        textLanguageDetectors = createPlatformTextLanguageDetectors(context.application),
        preferences = preferences,
        selectedEngine = engineResolver::resolve,
        scope = scope,
        audioFocus = audioFocus,
    )
    val hostActions = DefaultTtsHostActions(
        preferences = preferences,
        engineRegistry = registry,
        catalog = registry,
        setupRegistry = registry,
        engineResolver = engineResolver,
    )

    addSingletonFactory { preferences }
    addSingletonFactory<TtsEngineRegistry> { registry }
    addSingletonFactory<TtsEngineSetupRegistry> { registry }
    addSingletonFactory<KnownTtsEngineCatalog> { registry }
    addSingletonFactory<TtsFeature> { feature }
    addSingletonFactory<TtsHostActions> { hostActions }

    ApplicationFeatureRuntimeArtifacts(
        capabilityProviders = listOf(TtsEngineRegistryCapability.bind(registry)),
        runtimeBoundaries = listOf(applicationFeatureRuntimeBoundary<TtsFeature> { feature }),
        graphValidators = listOf(
            ApplicationFeatureRuntimeGraphValidator { evaluation ->
                TtsFeatureGraphStateValidator(evaluation).validate()
            },
        ),
    )
}
