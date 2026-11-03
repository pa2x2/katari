package mihon.tts.runtime.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mihon.language.api.identification.TextLanguageDetector
import mihon.tts.api.TtsFeature
import mihon.tts.api.host.TtsHostActions
import mihon.tts.runtime.audio.TtsAudioFocus
import mihon.tts.runtime.feature.DefaultTtsFeature
import mihon.tts.runtime.host.DefaultTtsHostActions
import mihon.tts.runtime.preference.ProfileTtsPreferences
import mihon.tts.runtime.registry.DefaultTtsEngineRegistry
import mihon.tts.runtime.selection.ProfileTtsEngineResolver
import mihon.tts.spi.contribution.TtsEngineContribution
import mihon.tts.spi.contribution.TtsProviderContribution
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller

@ContributesTo(AppScope::class)
interface TtsContributionMultibindings {
    /** Providers and their engines come from provider modules, and a build may include none. */
    @Multibinds(allowEmpty = true)
    fun ttsProviders(): Set<TtsProviderContribution>

    @Multibinds(allowEmpty = true)
    fun ttsEngines(): Set<TtsEngineContribution>
}

@ContributesTo(AppScope::class)
@BindingContainer
object TtsBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun registry(
        providers: Set<TtsProviderContribution>,
        engines: Set<TtsEngineContribution>,
    ): DefaultTtsEngineRegistry = DefaultTtsEngineRegistry(providers.toList(), engines.toList())

    @Provides
    @SingleIn(AppScope::class)
    fun preferencesOwner(
        owners: ProfilePreferenceOwnerInstaller,
        registry: DefaultTtsEngineRegistry,
    ): InstalledProfilePreferenceOwner<ProfileTtsPreferences> {
        val initialEngine = registry.engines.firstOrNull()?.catalogEntry?.id
        return owners.register(
            id = ProfilePreferenceOwnerId("tts"),
            keyPatterns = setOf(ProfileTtsPreferences.VOICE_KEY_FAMILY),
            factory = { store -> ProfileTtsPreferences(store, initialEngine) },
        )
    }

    @Provides
    @IntoSet
    fun preferencesOwnerRegistration(
        owner: InstalledProfilePreferenceOwner<ProfileTtsPreferences>,
    ): InstalledProfilePreferenceOwner<*> = owner

    @Provides
    @SingleIn(AppScope::class)
    fun preferences(owner: InstalledProfilePreferenceOwner<ProfileTtsPreferences>): ProfileTtsPreferences =
        owner.create()

    @Provides
    @SingleIn(AppScope::class)
    internal fun engineResolver(preferences: ProfileTtsPreferences): ProfileTtsEngineResolver =
        ProfileTtsEngineResolver(preferences)

    @Provides
    @SingleIn(AppScope::class)
    internal fun feature(
        registry: DefaultTtsEngineRegistry,
        preferences: ProfileTtsPreferences,
        engineResolver: ProfileTtsEngineResolver,
        textLanguageDetectors: Set<TextLanguageDetector>,
        audioFocus: TtsAudioFocus,
    ): TtsFeature = DefaultTtsFeature(
        engineRegistry = registry,
        knownEngineCatalog = registry,
        textLanguageDetectors = textLanguageDetectors.toList(),
        preferences = preferences,
        selectedEngine = engineResolver::resolve,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        audioFocus = audioFocus,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun hostActions(
        preferences: ProfileTtsPreferences,
        registry: DefaultTtsEngineRegistry,
        engineResolver: ProfileTtsEngineResolver,
    ): TtsHostActions = DefaultTtsHostActions(
        isOffered = registry.providers.isNotEmpty(),
        preferences = preferences,
        engineRegistry = registry,
        catalog = registry,
        setupRegistry = registry,
        engineResolver = engineResolver,
    )
}
