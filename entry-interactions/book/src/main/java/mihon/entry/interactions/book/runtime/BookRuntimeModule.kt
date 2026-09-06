package mihon.entry.interactions.book.runtime

import android.app.Application
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.book.content.BookMaterializationCache
import mihon.entry.interactions.book.content.BookMaterializationStore
import mihon.entry.interactions.book.document.preparation.BookDocumentPreparedCache
import mihon.entry.interactions.book.document.reader.BookDocumentReaderProcessor
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderPreferences
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderSettingsProvider
import mihon.entry.interactions.book.document.resource.BookPublicationResourceGatewayFactory
import mihon.entry.interactions.book.document.resource.BookRemoteResourceConsentPreferences
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.download.BookDownloadIndexStore
import mihon.entry.interactions.book.download.BookDownloadManager
import mihon.entry.interactions.book.download.BookDownloadProvider
import mihon.entry.interactions.book.download.BookDownloadStore
import mihon.entry.interactions.book.download.BookDownloader
import mihon.entry.interactions.book.format.epub.preparation.EpubBookPreparer
import mihon.entry.interactions.book.format.html.prosechapter.preparation.HtmlProseChapterPreparer
import mihon.entry.interactions.book.media.session.BookMediaSessionProcessor
import mihon.entry.interactions.book.navigation.BookChapterNavigationResolver
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry
import mihon.entry.interactions.book.processor.BookReaderProcessorPreferences
import mihon.entry.interactions.book.processor.BookReaderProcessorRegistry
import mihon.entry.interactions.book.processor.BookReaderProcessorSelectionCoordinator
import mihon.entry.interactions.book.reader.BookReaderHostResolver
import mihon.entry.interactions.book.reader.BookReaderSessionFactory
import mihon.entry.interactions.book.reader.BookReaderSessionRegistry
import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationPreferences
import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationSettingsProvider
import mihon.entry.interactions.media.DefaultEntryViewerSettingsProvider
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.reader.settings.BookDocumentReaderSettings
import mihon.entry.interactions.runtime.EntryTypeRuntimeContribution
import mihon.entry.interactions.runtime.EntryTypeRuntimeModule
import mihon.entry.viewer.settings.shared.ReaderCapabilityId
import mihon.translation.api.host.TranslationHostActions
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.storage.service.StorageManager
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

fun bookEntryTypeRuntimeModule(profilePreferenceOwners: ProfilePreferenceOwnerInstaller): EntryTypeRuntimeModule {
    return EntryTypeRuntimeModule(EntryType.BOOK) { app ->
        val runtime = addBookEntryInteractionRuntime(app, profilePreferenceOwners)
        val viewerSettingsProvider = get<BookDocumentReaderSettingsProvider>()
        val typeViewerSettingsProvider = DefaultEntryViewerSettingsProvider(
            type = EntryType.BOOK,
            surfaces = listOf(viewerSettingsProvider),
        )
        val mediaSession = BookMediaSessionProcessor(get<EntryMediaSessionEventSink>())
        addSingletonFactory { mediaSession }
        val progressRepository = get<EntryProgressRepository>()
        EntryTypeRuntimeContribution(
            plugin = bookEntryInteractionPlugin(
                BookEntryInteractionDependencies(
                    getEntryWithChapters = get(),
                    entryChapterRepository = get(),
                    entryProgressRepository = progressRepository,
                    downloadsEnabled = runtime.canPrepareContent,
                    mediaSession = mediaSession,
                ),
                viewerSettingsProvider = typeViewerSettingsProvider,
            ),
            potentialReaderCapabilitiesBySettingsSurface = runtime.potentialReaderCapabilitiesBySettingsSurface,
            sharedReaderSettingsProviderFactories = if (
                runtime.potentialReaderCapabilitiesBySettingsSurface.isEmpty()
            ) {
                emptyList()
            } else {
                listOf({ get<BookAutomaticTranslationSettingsProvider>() })
            },
        )
    }
}

/** Installs generic BOOK host services and composes built-in preparers with reader implementations. */
private fun InjektRegistrar.addBookEntryInteractionRuntime(
    app: Application,
    profilePreferenceOwners: ProfilePreferenceOwnerInstaller,
): BookRuntimeArtifacts {
    val materializationCache = BookMaterializationCache(app)
    val preparedDocumentCache = BookDocumentPreparedCache(app)
    val resourceGatewayFactory =
        BookPublicationResourceGatewayFactory(app, get<eu.kanade.tachiyomi.network.NetworkHelper>().client)
    val processorPreferencesOwner = profilePreferenceOwners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.book.processor-selection"),
        keyPatterns = BookReaderProcessorPreferences.profileKeyPatterns,
        factory = ::BookReaderProcessorPreferences,
    )
    val automaticTranslationPreferencesOwner = profilePreferenceOwners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.book.automatic-translation"),
        keyPatterns = setOf(
            ProfilePreferenceKeyPattern.Prefix(BookAutomaticTranslationPreferences.SURFACE_KEY_PREFIX),
        ),
        factory = ::BookAutomaticTranslationPreferences,
    )
    val documentReaderSettingsOwner = profilePreferenceOwners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.book.document-reader-settings"),
        keyPatterns = setOf(
            ProfilePreferenceKeyPattern.Prefix(BookDocumentReaderPreferences.KEY_PREFIX),
        ),
        factory = ::BookDocumentReaderPreferences,
    )
    val remoteResourceConsentOwner = profilePreferenceOwners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.book.remote-resource-consent"),
        keyPatterns = setOf(
            ProfilePreferenceKeyPattern.Prefix(BookRemoteResourceConsentPreferences.KEY_PREFIX),
        ),
        factory = ::BookRemoteResourceConsentPreferences,
    )
    val automaticTranslationPreferences = automaticTranslationPreferencesOwner.create()
    val documentReaderPreferences = documentReaderSettingsOwner.create()
    val remoteResourceConsentPreferences = remoteResourceConsentOwner.create()
    val documentReaderSettingsProvider = BookDocumentReaderSettingsProvider(
        preferences = documentReaderPreferences,
        chapterPreparationPreferences = get(),
        automaticTranslationPreferences = automaticTranslationPreferences,
    )
    val preparerRegistry = BookContentPreparerRegistry(
        preparers = listOf(
            HtmlProseChapterPreparer(),
            EpubBookPreparer(preparedDocumentCache, resourceGatewayFactory),
        ),
    )
    val readerProcessorRegistry = BookReaderProcessorRegistry(
        processors = listOf(BookDocumentReaderProcessor()),
    )
    addSingletonFactory { materializationCache }
    addSingletonFactory { preparedDocumentCache }
    addSingletonFactory { resourceGatewayFactory }
    addSingletonFactory { remoteResourceConsentPreferences }
    addSingletonFactory<BookMaterializationStore> { get<BookMaterializationCache>() }
    addSingletonFactory { BookDownloadProvider(get<StorageManager>(), app) }
    addSingletonFactory {
        val storageManager = get<StorageManager>()
        BookDownloadCache(
            provider = get(),
            indexStore = BookDownloadIndexStore(app),
            storageChanges = storageManager.changes,
        )
    }
    addSingletonFactory { BookDownloadStore(app) }
    addSingletonFactory { BookReaderSessionRegistry() }
    addSingletonFactory { BookChapterNavigationResolver(get()) }
    addSingletonFactory { automaticTranslationPreferences }
    addSingletonFactory { documentReaderPreferences }
    addSingletonFactory { documentReaderSettingsProvider }
    addSingletonFactory<BookDocumentReaderSettings> { get<BookDocumentReaderSettingsProvider>() }
    addSingletonFactory { preparerRegistry }
    addSingletonFactory { readerProcessorRegistry }
    addSingletonFactory {
        BookAutomaticTranslationSettingsProvider(
            processorRegistry = readerProcessorRegistry,
            preferences = automaticTranslationPreferences,
            translationHostActions = get<TranslationHostActions>(),
        )
    }
    addSingletonFactory {
        BookDownloader(
            application = app,
            provider = get(),
            cache = get(),
            sourceManager = get(),
            networkHelper = get(),
            materializationStore = get(),
            preparerRegistry = get(),
        )
    }
    addSingletonFactory {
        BookDownloadManager(
            context = app,
            cache = get(),
            provider = get(),
            downloader = get(),
            sourceManager = get(),
            store = get(),
        )
    }
    addSingletonFactory { processorPreferencesOwner.create() }
    addSingletonFactory {
        BookReaderProcessorSelectionCoordinator(
            registry = get(),
            preferences = get(),
        )
    }
    addSingletonFactory {
        BookReaderHostResolver(
            sessionFactory = get(),
            preparerRegistry = get(),
            selectionCoordinator = get(),
        )
    }
    addSingletonFactory {
        BookReaderSessionFactory(
            entryRepository = get(),
            entryChapterRepository = get(),
            entryProgressRepository = get(),
            sourceManager = get(),
            preparerRegistry = get(),
            readerProcessorRegistry = get(),
            networkHelper = get(),
            materializationStore = get(),
            downloadCache = get(),
            mediaSession = get<BookMediaSessionProcessor>(),
        )
    }
    return BookRuntimeArtifacts(
        canPrepareContent = preparerRegistry.canPrepareContent,
        potentialReaderCapabilitiesBySettingsSurface =
        readerProcessorRegistry.potentialReaderCapabilitiesBySettingsSurface(),
    )
}

private data class BookRuntimeArtifacts(
    val canPrepareContent: Boolean,
    val potentialReaderCapabilitiesBySettingsSurface: Map<String, Set<ReaderCapabilityId>>,
)
