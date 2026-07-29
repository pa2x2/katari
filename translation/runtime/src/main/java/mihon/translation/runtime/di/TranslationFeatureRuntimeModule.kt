package mihon.translation.runtime

import mihon.feature.runtime.ApplicationFeatureRuntimeArtifacts
import mihon.feature.runtime.ApplicationFeatureRuntimeGraphValidator
import mihon.feature.runtime.ApplicationFeatureRuntimeModule
import mihon.feature.runtime.applicationFeatureRuntimeBoundary
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationHostActions
import mihon.translation.runtime.selection.ProfileTranslationEngineResolver
import mihon.translation.runtime.system.AndroidSystemTranslationEngine
import mihon.translation.runtime.system.createAndroidSystemTranslationContribution
import mihon.translation.spi.KnownTranslationEngineCatalog
import mihon.translation.spi.TranslationEngineRegistry
import mihon.translation.spi.TranslationEngineSetupRegistry
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import uy.kohesive.injekt.api.addSingletonFactory

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
    val feature = DefaultTranslationFeature(
        engineRegistry = registry,
        knownEngineCatalog = registry,
        sourceLanguageDetectors = createTranslationSourceLanguageDetectors(
            application = context.application,
            contributions = runtimeContributions,
        ),
        defaultTargetLanguageResolver = ProfileTranslationDefaultTargetLanguageResolver(profilePreferences),
        selectedEngine = profileEngineResolver::resolve,
    )
    val hostActions = DefaultTranslationHostActions(
        preferences = profilePreferences,
        engineRegistry = registry,
        knownEngineCatalog = registry,
        setupRegistry = registry,
        profileEngineResolver = profileEngineResolver,
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
