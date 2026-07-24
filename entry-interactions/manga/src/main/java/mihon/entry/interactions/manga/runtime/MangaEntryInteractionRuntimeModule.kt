package mihon.entry.interactions.manga

import android.app.Application
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.DefaultEntryViewerSettingsProvider
import mihon.entry.interactions.ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID
import mihon.entry.interactions.EntryImageComponentInstaller
import mihon.entry.interactions.EntryMediaSessionEventSink
import mihon.entry.interactions.EntryTypeRuntimeContribution
import mihon.entry.interactions.EntryTypeRuntimeModule
import mihon.entry.interactions.manga.download.DownloadCache
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.download.DownloadProvider
import mihon.entry.interactions.manga.reader.addMangaReaderImageComponents
import mihon.entry.interactions.reader.settings.MangaReaderSettingsProvider
import mihon.entry.interactions.settings.EntryInteractionPreferences
import tachiyomi.core.common.preference.ProfilePreferenceOwnerGroupId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.entry.repository.EntryProgressRepository
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

fun mangaEntryTypeRuntimeModule(profilePreferenceOwners: ProfilePreferenceOwnerInstaller): EntryTypeRuntimeModule {
    return EntryTypeRuntimeModule(EntryType.MANGA) { app ->
        val warmup = addMangaEntryInteractionRuntime(app)
        val viewerSettingsOwner = profilePreferenceOwners.register(
            id = ProfilePreferenceOwnerId("entry-interactions.manga.viewer-settings"),
            groups = setOf(
                ProfilePreferenceOwnerGroupId(ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID),
            ),
            factory = ::MangaReaderSettingsProvider,
        )
        val viewerSettingsProvider = viewerSettingsOwner.create()
        val typeViewerSettingsProvider = DefaultEntryViewerSettingsProvider(
            type = EntryType.MANGA,
            surfaces = listOf(viewerSettingsProvider),
            legacyViewerFlagsNormalization = { flags -> flags and LEGACY_MANGA_VIEWER_MASK.inv() },
        )
        addSingletonFactory { viewerSettingsProvider }
        val progressRepository = get<EntryProgressRepository>()
        val mediaSession = MangaMediaSessionProcessor(get<EntryMediaSessionEventSink>())
        addSingletonFactory { mediaSession }
        EntryTypeRuntimeContribution(
            plugin = mangaEntryInteractionPlugin(
                MangaEntryInteractionDependencies(
                    getEntryWithChapters = get(),
                    entryChapterRepository = get(),
                    entryProgressRepository = progressRepository,
                    downloadPreferences = get(),
                    sourceManager = get(),
                    mediaSession = mediaSession,
                    entryInteractionPreferences = get<EntryInteractionPreferences>(),
                ),
                viewerSettingsProvider = typeViewerSettingsProvider,
            ),
            warmups = listOf(warmup),
            imageComponentInstallers = listOf(EntryImageComponentInstaller(::addMangaReaderImageComponents)),
        )
    }
}

private const val LEGACY_MANGA_VIEWER_MASK = 0x3FL

private fun InjektRegistrar.addMangaEntryInteractionRuntime(app: Application): () -> Unit {
    addSingletonFactory { DownloadProvider(app) }
    addSingletonFactory { DownloadManager(app) }
    addSingletonFactory { DownloadCache(app) }

    return { get<DownloadManager>() }
}
