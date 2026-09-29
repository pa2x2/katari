package mihon.translation.runtime.di

import mihon.feature.runtime.application.ApplicationFeatureRuntimeArtifacts
import mihon.feature.runtime.application.ApplicationFeatureRuntimeGraphValidator
import mihon.feature.runtime.application.ApplicationFeatureRuntimeModule
import mihon.feature.runtime.application.applicationFeatureRuntimeBoundary
import mihon.language.runtime.identification.createPlatformTextLanguageDetectors
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.runtime.cache.TranslationResultCache
import mihon.translation.runtime.component.TranslationRuntimeContribution
import mihon.translation.runtime.component.createTranslationRuntimeContributions
import mihon.translation.runtime.feature.DefaultTranslationFeature
import mihon.translation.runtime.graph.TranslationEngineRegistryCapability
import mihon.translation.runtime.graph.TranslationFeatureContributor
import mihon.translation.runtime.graph.TranslationFeatureGraphStateValidator
import mihon.translation.runtime.host.DefaultTranslationHostActions
import mihon.translation.runtime.language.ProfileTranslationDefaultTargetLanguageResolver
import mihon.translation.runtime.preference.ProfileTranslationPreferences
import mihon.translation.runtime.registry.DefaultTranslationEngineRegistry
import mihon.translation.runtime.selection.ProfileTranslationEngineResolver
import mihon.translation.runtime.system.AndroidSystemTranslationEngine
import mihon.translation.runtime.system.createAndroidSystemTranslationContribution
import mihon.translation.spi.engine.KnownTranslationEngineCatalog
import mihon.translation.spi.engine.TranslationEngineRegistry
import mihon.translation.spi.setup.TranslationEngineSetupRegistry
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import uy.kohesive.injekt.api.addSingletonFactory
import java.io.File

val translationFeatureRuntimeModule = ApplicationFeatureRuntimeModule(
    id = "translation",
    contributor = TranslationFeatureContributor,
) { context ->
    val profilePreferencesOwner = context.dependencies.profilePreferenceOwners.register(
        id = ProfilePreferenceOwnerId("translation"),
        factory = {
            ProfileTranslationPreferences(
                preferenceStore = it,
                defaultEngine = AndroidSystemTranslationEngine.ENGINE_ID,
            )
        },
    )
    val profilePreferences = profilePreferencesOwner.create()
    val runtimeContributions = createTranslationRuntimeContributions(
        application = context.application,
        components = context.components,
    )
    val contributions = buildList {
        add(createAndroidSystemTranslationContribution(context.application))
        runtimeContributions.flatMapTo(this, TranslationRuntimeContribution::engineContributions)
    }
    val registry = DefaultTranslationEngineRegistry(contributions)
    val profileEngineResolver = ProfileTranslationEngineResolver(
        preferences = profilePreferences,
        engineRegistry = registry,
    )
    val defaultTargetResolver = ProfileTranslationDefaultTargetLanguageResolver(profilePreferences)
    val feature = DefaultTranslationFeature(
        engineRegistry = registry,
        knownEngineCatalog = registry,
        textLanguageDetectors = createPlatformTextLanguageDetectors(context.application),
        defaultTargetLanguageResolver = defaultTargetResolver,
        selectedEngine = profileEngineResolver::resolve,
        resultCache = TranslationResultCache(
            directory = { File(context.application.cacheDir, TRANSLATION_CACHE_DIRECTORY) },
            maximumBytes = TRANSLATION_CACHE_MAXIMUM_BYTES,
        ),
    )
    val hostActions = DefaultTranslationHostActions(
        preferences = profilePreferences,
        engineRegistry = registry,
        knownEngineCatalog = registry,
        setupRegistry = registry,
        profileEngineResolver = profileEngineResolver,
        defaultTargetResolver = defaultTargetResolver,
    )

    addSingletonFactory { profilePreferences }
    addSingletonFactory<TranslationEngineRegistry> { registry }
    addSingletonFactory<TranslationEngineSetupRegistry> { registry }
    addSingletonFactory<KnownTranslationEngineCatalog> { registry }
    addSingletonFactory<TranslationFeature> { feature }
    addSingletonFactory<TranslationHostActions> { hostActions }

    ApplicationFeatureRuntimeArtifacts(
        capabilityProviders = listOf(TranslationEngineRegistryCapability.bind(registry)),
        runtimeBoundaries = listOf(
            applicationFeatureRuntimeBoundary<TranslationFeature> { feature },
        ),
        graphValidators = listOf(
            ApplicationFeatureRuntimeGraphValidator { evaluation ->
                TranslationFeatureGraphStateValidator(evaluation).validate()
            },
        ),
    )
}

private const val TRANSLATION_CACHE_DIRECTORY = "translation"

/** Translations are short texts, so this keeps tens of thousands of them. */
private const val TRANSLATION_CACHE_MAXIMUM_BYTES = 8L * 1024 * 1024
