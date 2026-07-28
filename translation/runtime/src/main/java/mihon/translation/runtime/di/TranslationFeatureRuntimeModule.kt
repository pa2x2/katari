package mihon.translation.runtime

import mihon.feature.runtime.ApplicationFeatureRuntimeArtifacts
import mihon.feature.runtime.ApplicationFeatureRuntimeGraphValidator
import mihon.feature.runtime.ApplicationFeatureRuntimeModule
import mihon.feature.runtime.applicationFeatureRuntimeBoundary
import mihon.translation.api.TranslationFeature
import mihon.translation.runtime.system.AndroidSystemTranslationEngine
import mihon.translation.runtime.system.DefaultAndroidSystemTranslationPlatform
import mihon.translation.runtime.system.createAndroidTranslationManagerBridge
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
        factory = ::ProfileTranslationPreferences,
    )
    val profilePreferences = profilePreferencesOwner.create()
    val devicePreferences = DeviceTranslationPreferences(context.dependencies.basePreferenceStore)
    val androidSystemEngine = AndroidSystemTranslationEngine(
        DefaultAndroidSystemTranslationPlatform(
            sdkInt = android.os.Build.VERSION.SDK_INT,
            bridge = createAndroidTranslationManagerBridge(context.application),
        ),
    )
    val registry = DefaultTranslationEngineRegistry(engines = listOf(androidSystemEngine))
    val setupRegistry = DefaultTranslationEngineSetupRegistry(setups = listOf(androidSystemEngine))
    val feature = DefaultTranslationFeature(
        engineRegistry = registry,
        knownEngineCatalog = registry,
        sourceLanguageDetectors = createTranslationSourceLanguageDetectors(
            application = context.application,
            components = context.components,
        ),
        defaultTargetLanguageResolver = ProfileTranslationDefaultTargetLanguageResolver(profilePreferences),
        preferredEngineSelection = profilePreferences.engineSelection::get,
    )

    addSingletonFactory { profilePreferences }
    addSingletonFactory { devicePreferences }
    addSingletonFactory<TranslationEngineRegistry> { registry }
    addSingletonFactory<TranslationEngineSetupRegistry> { setupRegistry }
    addSingletonFactory<KnownTranslationEngineCatalog> { registry }
    addSingletonFactory<TranslationFeature> { feature }

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
