package mihon.tts.runtime.di

import android.app.Application
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
import mihon.language.runtime.identification.createPlatformTextLanguageDetectors
import mihon.tts.api.TtsFeature
import mihon.tts.api.host.TtsHostActions
import mihon.tts.runtime.audio.AndroidTtsAudioFocus
import mihon.tts.runtime.feature.DefaultTtsFeature
import mihon.tts.runtime.host.DefaultTtsHostActions
import mihon.tts.runtime.preference.ProfileTtsPreferences
import mihon.tts.runtime.registry.DefaultTtsEngineRegistry
import mihon.tts.runtime.selection.ProfileTtsEngineResolver
import mihon.tts.spi.contribution.TtsEngineContribution
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller

@ContributesTo(AppScope::class)
interface TtsEngineMultibindings {
    /** Engines come from provider modules, and a build may include none. */
    @Multibinds(allowEmpty = true)
    fun engines(): Set<TtsEngineContribution>
}

@ContributesTo(AppScope::class)
@BindingContainer
object TtsBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun registry(engines: Set<TtsEngineContribution>): DefaultTtsEngineRegistry =
        DefaultTtsEngineRegistry(engines.toList())

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
        application: Application,
        registry: DefaultTtsEngineRegistry,
        preferences: ProfileTtsPreferences,
        engineResolver: ProfileTtsEngineResolver,
    ): TtsFeature {
        lateinit var feature: DefaultTtsFeature
        val audioFocus = AndroidTtsAudioFocus(application) {
            feature.onAudioFocusLost()
        }
        feature = DefaultTtsFeature(
            engineRegistry = registry,
            knownEngineCatalog = registry,
            textLanguageDetectors = createPlatformTextLanguageDetectors(application),
            preferences = preferences,
            selectedEngine = engineResolver::resolve,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            audioFocus = audioFocus,
        )
        return feature
    }

    @Provides
    @SingleIn(AppScope::class)
    internal fun hostActions(
        preferences: ProfileTtsPreferences,
        registry: DefaultTtsEngineRegistry,
        engineResolver: ProfileTtsEngineResolver,
    ): TtsHostActions = DefaultTtsHostActions(
        preferences = preferences,
        engineRegistry = registry,
        catalog = registry,
        setupRegistry = registry,
        engineResolver = engineResolver,
    )
}
