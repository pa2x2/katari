package mihon.translation.runtime.di

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.FeatureGraphValidator
import mihon.language.runtime.identification.createPlatformTextLanguageDetectors
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.runtime.cache.TranslationResultCache
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
import mihon.translation.spi.contribution.TranslationEngineContribution
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import java.io.File

@ContributesTo(AppScope::class)
@BindingContainer
object TranslationBindings {
    @Provides
    @IntoSet
    fun systemEngine(application: Application): TranslationEngineContribution =
        createAndroidSystemTranslationContribution(application)

    @Provides
    @SingleIn(AppScope::class)
    fun registry(engines: Set<TranslationEngineContribution>): DefaultTranslationEngineRegistry =
        DefaultTranslationEngineRegistry(engines.toList())

    @Provides
    @SingleIn(AppScope::class)
    fun preferencesOwner(
        owners: ProfilePreferenceOwnerInstaller,
    ): InstalledProfilePreferenceOwner<ProfileTranslationPreferences> = owners.register(
        id = ProfilePreferenceOwnerId("translation"),
        factory = {
            ProfileTranslationPreferences(
                preferenceStore = it,
                defaultEngine = AndroidSystemTranslationEngine.ENGINE_ID,
            )
        },
    )

    @Provides
    @IntoSet
    fun preferencesOwnerRegistration(
        owner: InstalledProfilePreferenceOwner<ProfileTranslationPreferences>,
    ): InstalledProfilePreferenceOwner<*> = owner

    @Provides
    @SingleIn(AppScope::class)
    fun preferences(
        owner: InstalledProfilePreferenceOwner<ProfileTranslationPreferences>,
    ): ProfileTranslationPreferences = owner.create()

    @Provides
    @SingleIn(AppScope::class)
    internal fun engineResolver(
        preferences: ProfileTranslationPreferences,
        registry: DefaultTranslationEngineRegistry,
    ): ProfileTranslationEngineResolver = ProfileTranslationEngineResolver(
        preferences = preferences,
        engineRegistry = registry,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun defaultTargetResolver(
        preferences: ProfileTranslationPreferences,
    ): ProfileTranslationDefaultTargetLanguageResolver = ProfileTranslationDefaultTargetLanguageResolver(preferences)

    @Provides
    @SingleIn(AppScope::class)
    internal fun feature(
        application: Application,
        registry: DefaultTranslationEngineRegistry,
        engineResolver: ProfileTranslationEngineResolver,
        defaultTargetResolver: ProfileTranslationDefaultTargetLanguageResolver,
    ): TranslationFeature = DefaultTranslationFeature(
        engineRegistry = registry,
        knownEngineCatalog = registry,
        textLanguageDetectors = createPlatformTextLanguageDetectors(application),
        defaultTargetLanguageResolver = defaultTargetResolver,
        selectedEngine = engineResolver::resolve,
        resultCache = TranslationResultCache(
            directory = { File(application.cacheDir, TRANSLATION_CACHE_DIRECTORY) },
            maximumBytes = TRANSLATION_CACHE_MAXIMUM_BYTES,
        ),
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun hostActions(
        preferences: ProfileTranslationPreferences,
        registry: DefaultTranslationEngineRegistry,
        engineResolver: ProfileTranslationEngineResolver,
        defaultTargetResolver: ProfileTranslationDefaultTargetLanguageResolver,
    ): TranslationHostActions = DefaultTranslationHostActions(
        preferences = preferences,
        engineRegistry = registry,
        knownEngineCatalog = registry,
        setupRegistry = registry,
        profileEngineResolver = engineResolver,
        defaultTargetResolver = defaultTargetResolver,
    )

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = TranslationFeatureContributor

    @Provides
    @IntoSet
    fun engineRegistryCapability(registry: DefaultTranslationEngineRegistry): CapabilityProvider<*> =
        TranslationEngineRegistryCapability.bind(registry)

    @Provides
    @IntoSet
    fun graphValidator(): FeatureGraphValidator = FeatureGraphValidator { evaluation ->
        TranslationFeatureGraphStateValidator(evaluation).validate()
    }
}

private const val TRANSLATION_CACHE_DIRECTORY = "translation"

/** Translations are short texts, so this keeps tens of thousands of them. */
private const val TRANSLATION_CACHE_MAXIMUM_BYTES = 8L * 1024 * 1024
