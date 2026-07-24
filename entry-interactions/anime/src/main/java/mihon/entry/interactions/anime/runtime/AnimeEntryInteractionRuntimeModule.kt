package mihon.entry.interactions.anime

import android.app.Application
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.DefaultEntryViewerSettingsProvider
import mihon.entry.interactions.ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID
import mihon.entry.interactions.EntryMediaSessionEventSink
import mihon.entry.interactions.EntryTypeRuntimeContribution
import mihon.entry.interactions.EntryTypeRuntimeModule
import mihon.entry.interactions.anime.download.AnimeDownloadCache
import mihon.entry.interactions.anime.download.AnimeDownloadManager
import mihon.entry.interactions.anime.download.AnimeDownloadProvider
import mihon.entry.interactions.anime.download.AnimeDownloader
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.interactions.settings.EntryInteractionPreferences
import tachiyomi.core.common.preference.ProfilePreferenceOwnerGroupId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.entry.repository.EntryProgressRepository
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

fun animeEntryTypeRuntimeModule(profilePreferenceOwners: ProfilePreferenceOwnerInstaller): EntryTypeRuntimeModule {
    return EntryTypeRuntimeModule(EntryType.ANIME) { app ->
        val warmup = addAnimeEntryInteractionRuntime(app)
        val viewerSettingsOwner = profilePreferenceOwners.register(
            id = ProfilePreferenceOwnerId("entry-interactions.anime.viewer-settings"),
            groups = setOf(
                ProfilePreferenceOwnerGroupId(ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID),
            ),
            factory = ::AnimePlayerPreferences,
        )
        val viewerSettingsProvider = viewerSettingsOwner.create()
        val typeViewerSettingsProvider = DefaultEntryViewerSettingsProvider(
            type = EntryType.ANIME,
            surfaces = listOf(viewerSettingsProvider),
        )
        addSingletonFactory { viewerSettingsProvider }
        val progressRepository = get<EntryProgressRepository>()
        val mediaSession = AnimeMediaSessionProcessor(get<EntryMediaSessionEventSink>())
        addSingletonFactory { mediaSession }
        EntryTypeRuntimeContribution(
            plugin = animeEntryInteractionPlugin(
                AnimeEntryInteractionDependencies(
                    entryChapterRepository = get(),
                    getEntryWithChapters = get(),
                    entryProgressRepository = progressRepository,
                    playbackPreferencesRepository = get(),
                    downloadPreferences = get(),
                    downloadPreferencesRepository = get(),
                    sourceManager = get(),
                    entryRepository = get(),
                    mediaSession = mediaSession,
                    entryInteractionPreferences = get<EntryInteractionPreferences>(),
                ),
                viewerSettingsProvider = typeViewerSettingsProvider,
            ),
            warmups = listOf(warmup),
        )
    }
}

private fun InjektRegistrar.addAnimeEntryInteractionRuntime(app: Application): () -> Unit {
    addAnimePlayerRuntime(app)
    addSingletonFactory { AnimeDownloadProvider(app) }
    addSingletonFactory { AnimeDownloadCache(app) }
    addSingletonFactory { AnimeDownloader(get(), get(), get(), get(), get(), get()) }
    addSingletonFactory { AnimeDownloadManager(app, get(), get(), get(), get()) }

    return { get<AnimeDownloadManager>() }
}
