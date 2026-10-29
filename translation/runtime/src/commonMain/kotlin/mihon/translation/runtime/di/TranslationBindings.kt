package mihon.translation.runtime.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.core.platform.locale.AppLocaleStore
import mihon.core.platform.storage.AppPrivateDirectories
import mihon.language.api.identification.TextLanguageDetector
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.runtime.cache.TranslationResultCache
import mihon.translation.runtime.feature.DefaultTranslationFeature
import mihon.translation.runtime.host.DefaultTranslationHostActions
import mihon.translation.runtime.language.ProfileTranslationDefaultTargetLanguageResolver
import mihon.translation.runtime.preference.DeviceDefaultTranslationEngine
import mihon.translation.runtime.preference.ProfileTranslationPreferences
import mihon.translation.runtime.registry.DefaultTranslationEngineRegistry
import mihon.translation.runtime.selection.ProfileTranslationEngineResolver
import mihon.translation.spi.contribution.TranslationEngineContribution
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import java.io.File

@ContributesTo(AppScope::class)
interface TranslationEngineMultibindings {
    /** The platform's own translator is the default where there is one; a platform without one contributes none. */
    @Multibinds(allowEmpty = true)
    fun deviceDefaultEngines(): Set<DeviceDefaultTranslationEngine>
}

@ContributesTo(AppScope::class)
@BindingContainer
object TranslationBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun registry(engines: Set<TranslationEngineContribution>): DefaultTranslationEngineRegistry =
        DefaultTranslationEngineRegistry(engines.toList())

    @Provides
    @SingleIn(AppScope::class)
    fun preferencesOwner(
        owners: ProfilePreferenceOwnerInstaller,
        deviceDefaultEngines: Set<DeviceDefaultTranslationEngine>,
    ): InstalledProfilePreferenceOwner<ProfileTranslationPreferences> {
        val defaultEngine = deviceDefaultEngines.singleOrNull()?.engine ?: DeviceDefaultTranslationEngine.NONE
        return owners.register(
            id = ProfilePreferenceOwnerId("translation"),
            factory = { ProfileTranslationPreferences(preferenceStore = it, defaultEngine = defaultEngine) },
        )
    }

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
        appLocale: AppLocaleStore,
    ): ProfileTranslationDefaultTargetLanguageResolver = ProfileTranslationDefaultTargetLanguageResolver(
        preferences = preferences,
        effectiveUiLocale = { appLocale.current },
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun feature(
        directories: AppPrivateDirectories,
        textLanguageDetectors: Set<TextLanguageDetector>,
        registry: DefaultTranslationEngineRegistry,
        engineResolver: ProfileTranslationEngineResolver,
        defaultTargetResolver: ProfileTranslationDefaultTargetLanguageResolver,
    ): TranslationFeature = DefaultTranslationFeature(
        engineRegistry = registry,
        knownEngineCatalog = registry,
        textLanguageDetectors = textLanguageDetectors.toList(),
        defaultTargetLanguageResolver = defaultTargetResolver,
        selectedEngine = engineResolver::resolve,
        resultCache = TranslationResultCache(
            directory = { File(directories.cache, TRANSLATION_CACHE_DIRECTORY) },
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
}

private const val TRANSLATION_CACHE_DIRECTORY = "translation"

/** Translations are short texts, so this keeps tens of thousands of them. */
private const val TRANSLATION_CACHE_MAXIMUM_BYTES = 8L * 1024 * 1024
