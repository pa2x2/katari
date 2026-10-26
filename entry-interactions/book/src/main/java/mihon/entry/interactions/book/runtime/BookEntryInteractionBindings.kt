package mihon.entry.interactions.book.runtime

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import mihon.core.metro.AppCoroutineScope
import mihon.entry.interactions.book.content.BookMaterializationCache
import mihon.entry.interactions.book.content.BookMaterializationStore
import mihon.entry.interactions.book.document.preparation.BookDocumentPreparedCache
import mihon.entry.interactions.book.document.reader.BookDocumentReaderProcessor
import mihon.entry.interactions.book.document.reader.font.BookDocumentReaderFontStore
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
import mihon.entry.interactions.download.EntryDownloadWorkController
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.interactions.reader.settings.BookDocumentReaderFontLibrary
import mihon.entry.interactions.reader.settings.BookDocumentReaderSettings
import mihon.entry.interactions.runtime.ReaderSettingsSurfaceCapabilities
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsProvider
import mihon.translation.api.host.TranslationHostActions
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager

/** Generic BOOK host services, composing built-in preparers with reader implementations. */
@ContributesTo(AppScope::class)
@BindingContainer
object BookEntryInteractionBindings {

    @Provides
    @ElementsIntoSet
    internal fun readerSurfaceCapabilities(
        registry: BookReaderProcessorRegistry,
    ): Set<ReaderSettingsSurfaceCapabilities> {
        val onByDefault = registry.sharedSettingsOnByDefaultBySettingsSurface()
        return registry.potentialReaderCapabilitiesBySettingsSurface()
            .map { (surfaceId, capabilities) ->
                ReaderSettingsSurfaceCapabilities(surfaceId, capabilities, onByDefault[surfaceId].orEmpty())
            }
            .toSet()
    }

    @Provides
    @ElementsIntoSet
    internal fun sharedReaderSettingsProviders(
        registry: BookReaderProcessorRegistry,
        provider: () -> BookAutomaticTranslationSettingsProvider,
    ): Set<ReaderSharedSettingsProvider> = if (registry.potentialReaderCapabilitiesBySettingsSurface().isEmpty()) {
        emptySet()
    } else {
        setOf(provider())
    }

    @Provides
    @SingleIn(AppScope::class)
    internal fun mediaSessionProcessor(sink: EntryMediaSessionEventSink): BookMediaSessionProcessor =
        BookMediaSessionProcessor(sink)

    @Provides
    @SingleIn(AppScope::class)
    internal fun materializationCache(application: Application): BookMaterializationCache =
        BookMaterializationCache(application)

    @Provides
    internal fun materializationStore(cache: BookMaterializationCache): BookMaterializationStore = cache

    @Provides
    @SingleIn(AppScope::class)
    internal fun preparedDocumentCache(application: Application): BookDocumentPreparedCache =
        BookDocumentPreparedCache(application)

    @Provides
    @SingleIn(AppScope::class)
    internal fun resourceGatewayFactory(
        application: Application,
        networkHelper: NetworkHelper,
    ): BookPublicationResourceGatewayFactory = BookPublicationResourceGatewayFactory(application, networkHelper.client)

    @Provides
    @SingleIn(AppScope::class)
    internal fun processorPreferencesOwner(
        owners: ProfilePreferenceOwnerInstaller,
    ): InstalledProfilePreferenceOwner<BookReaderProcessorPreferences> = owners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.book.processor-selection"),
        keyPatterns = BookReaderProcessorPreferences.profileKeyPatterns,
        factory = ::BookReaderProcessorPreferences,
    )

    @Provides
    @IntoSet
    internal fun processorPreferencesOwnerRegistration(
        owner: InstalledProfilePreferenceOwner<BookReaderProcessorPreferences>,
    ): InstalledProfilePreferenceOwner<*> = owner

    @Provides
    @SingleIn(AppScope::class)
    internal fun processorPreferences(
        owner: InstalledProfilePreferenceOwner<BookReaderProcessorPreferences>,
    ): BookReaderProcessorPreferences = owner.create()

    @Provides
    @SingleIn(AppScope::class)
    internal fun automaticTranslationPreferencesOwner(
        owners: ProfilePreferenceOwnerInstaller,
    ): InstalledProfilePreferenceOwner<BookAutomaticTranslationPreferences> = owners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.book.automatic-translation"),
        keyPatterns = setOf(
            ProfilePreferenceKeyPattern.Prefix(BookAutomaticTranslationPreferences.SURFACE_KEY_PREFIX),
        ),
        factory = ::BookAutomaticTranslationPreferences,
    )

    @Provides
    @IntoSet
    internal fun automaticTranslationPreferencesOwnerRegistration(
        owner: InstalledProfilePreferenceOwner<BookAutomaticTranslationPreferences>,
    ): InstalledProfilePreferenceOwner<*> = owner

    @Provides
    @SingleIn(AppScope::class)
    internal fun automaticTranslationPreferences(
        owner: InstalledProfilePreferenceOwner<BookAutomaticTranslationPreferences>,
    ): BookAutomaticTranslationPreferences = owner.create()

    @Provides
    @SingleIn(AppScope::class)
    internal fun documentReaderPreferencesOwner(
        owners: ProfilePreferenceOwnerInstaller,
    ): InstalledProfilePreferenceOwner<BookDocumentReaderPreferences> = owners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.book.document-reader-settings"),
        keyPatterns = setOf(
            ProfilePreferenceKeyPattern.Prefix(BookDocumentReaderPreferences.KEY_PREFIX),
        ),
        factory = ::BookDocumentReaderPreferences,
    )

    @Provides
    @IntoSet
    internal fun documentReaderPreferencesOwnerRegistration(
        owner: InstalledProfilePreferenceOwner<BookDocumentReaderPreferences>,
    ): InstalledProfilePreferenceOwner<*> = owner

    @Provides
    @SingleIn(AppScope::class)
    internal fun documentReaderPreferences(
        owner: InstalledProfilePreferenceOwner<BookDocumentReaderPreferences>,
    ): BookDocumentReaderPreferences = owner.create()

    @Provides
    @SingleIn(AppScope::class)
    internal fun remoteResourceConsentPreferencesOwner(
        owners: ProfilePreferenceOwnerInstaller,
    ): InstalledProfilePreferenceOwner<BookRemoteResourceConsentPreferences> = owners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.book.remote-resource-consent"),
        keyPatterns = setOf(
            ProfilePreferenceKeyPattern.Prefix(BookRemoteResourceConsentPreferences.KEY_PREFIX),
        ),
        factory = ::BookRemoteResourceConsentPreferences,
    )

    @Provides
    @IntoSet
    internal fun remoteResourceConsentPreferencesOwnerRegistration(
        owner: InstalledProfilePreferenceOwner<BookRemoteResourceConsentPreferences>,
    ): InstalledProfilePreferenceOwner<*> = owner

    @Provides
    @SingleIn(AppScope::class)
    internal fun remoteResourceConsentPreferences(
        owner: InstalledProfilePreferenceOwner<BookRemoteResourceConsentPreferences>,
    ): BookRemoteResourceConsentPreferences = owner.create()

    @Provides
    @SingleIn(AppScope::class)
    internal fun documentReaderSettingsProvider(
        preferences: BookDocumentReaderPreferences,
        chapterPreparationPreferences: ReaderChapterPreparationPreferences,
        automaticTranslationPreferences: BookAutomaticTranslationPreferences,
    ): BookDocumentReaderSettingsProvider = BookDocumentReaderSettingsProvider(
        preferences = preferences,
        chapterPreparationPreferences = chapterPreparationPreferences,
        automaticTranslationPreferences = automaticTranslationPreferences,
    )

    @Provides
    internal fun documentReaderSettings(provider: BookDocumentReaderSettingsProvider): BookDocumentReaderSettings =
        provider

    @Provides
    @SingleIn(AppScope::class)
    internal fun documentReaderFontStore(application: Application): BookDocumentReaderFontStore =
        BookDocumentReaderFontStore(application)

    @Provides
    internal fun documentReaderFontLibrary(store: BookDocumentReaderFontStore): BookDocumentReaderFontLibrary = store

    @Provides
    @SingleIn(AppScope::class)
    internal fun preparerRegistry(
        preparedDocumentCache: BookDocumentPreparedCache,
        resourceGatewayFactory: BookPublicationResourceGatewayFactory,
    ): BookContentPreparerRegistry = BookContentPreparerRegistry(
        preparers = listOf(
            HtmlProseChapterPreparer(),
            EpubBookPreparer(preparedDocumentCache, resourceGatewayFactory),
        ),
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun readerProcessorRegistry(): BookReaderProcessorRegistry = BookReaderProcessorRegistry(
        processors = listOf(BookDocumentReaderProcessor()),
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun automaticTranslationSettingsProvider(
        processorRegistry: BookReaderProcessorRegistry,
        preferences: BookAutomaticTranslationPreferences,
        translationHostActions: TranslationHostActions,
    ): BookAutomaticTranslationSettingsProvider = BookAutomaticTranslationSettingsProvider(
        processorRegistry = processorRegistry,
        preferences = preferences,
        translationHostActions = translationHostActions,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadProvider(storageManager: StorageManager, application: Application): BookDownloadProvider =
        BookDownloadProvider(storageManager, application)

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadCache(
        provider: BookDownloadProvider,
        @AppCoroutineScope scope: CoroutineScope,
        application: Application,
        storageManager: StorageManager,
    ): BookDownloadCache = BookDownloadCache(
        provider = provider,
        scope = scope,
        indexStore = BookDownloadIndexStore(application),
        storageChanges = storageManager.changes,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadStore(
        application: Application,
        json: Json,
        entryRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
    ): BookDownloadStore = BookDownloadStore(application, json, entryRepository, entryChapterRepository)

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloader(
        application: Application,
        provider: BookDownloadProvider,
        cache: BookDownloadCache,
        sourceManager: SourceManager,
        networkHelper: NetworkHelper,
        materializationStore: BookMaterializationStore,
        preparerRegistry: BookContentPreparerRegistry,
    ): BookDownloader = BookDownloader(
        application = application,
        provider = provider,
        cache = cache,
        sourceManager = sourceManager,
        networkHelper = networkHelper,
        materializationStore = materializationStore,
        preparerRegistry = preparerRegistry,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadManager(
        cache: BookDownloadCache,
        provider: BookDownloadProvider,
        downloader: BookDownloader,
        preparedDocumentCache: BookDocumentPreparedCache,
        resourceGatewayFactory: BookPublicationResourceGatewayFactory,
        sourceManager: SourceManager,
        store: BookDownloadStore,
        workController: EntryDownloadWorkController,
        @AppCoroutineScope scope: CoroutineScope,
    ): BookDownloadManager = BookDownloadManager(
        cache = cache,
        provider = provider,
        downloader = downloader,
        preparedDocumentCache = preparedDocumentCache,
        resourceGatewayFactory = resourceGatewayFactory,
        sourceManager = sourceManager,
        store = store,
        workController = workController,
        scope = scope,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun readerSessionRegistry(): BookReaderSessionRegistry = BookReaderSessionRegistry()

    @Provides
    @SingleIn(AppScope::class)
    internal fun chapterNavigationResolver(getEntryWithChapters: GetEntryWithChapters): BookChapterNavigationResolver =
        BookChapterNavigationResolver(getEntryWithChapters)

    @Provides
    @SingleIn(AppScope::class)
    internal fun processorSelectionCoordinator(
        registry: BookReaderProcessorRegistry,
        preferences: BookReaderProcessorPreferences,
    ): BookReaderProcessorSelectionCoordinator = BookReaderProcessorSelectionCoordinator(
        registry = registry,
        preferences = preferences,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun readerHostResolver(
        sessionFactory: BookReaderSessionFactory,
        preparerRegistry: BookContentPreparerRegistry,
        selectionCoordinator: BookReaderProcessorSelectionCoordinator,
    ): BookReaderHostResolver = BookReaderHostResolver(
        sessionFactory = sessionFactory,
        preparerRegistry = preparerRegistry,
        selectionCoordinator = selectionCoordinator,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun readerSessionFactory(
        entryRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
        entryProgressRepository: EntryProgressRepository,
        sourceManager: SourceManager,
        preparerRegistry: BookContentPreparerRegistry,
        readerProcessorRegistry: BookReaderProcessorRegistry,
        networkHelper: NetworkHelper,
        materializationStore: BookMaterializationStore,
        downloadCache: BookDownloadCache,
        mediaSession: BookMediaSessionProcessor,
    ): BookReaderSessionFactory = BookReaderSessionFactory(
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
        entryProgressRepository = entryProgressRepository,
        sourceManager = sourceManager,
        preparerRegistry = preparerRegistry,
        readerProcessorRegistry = readerProcessorRegistry,
        networkHelper = networkHelper,
        materializationStore = materializationStore,
        downloadCache = downloadCache,
        mediaSession = mediaSession,
    )
}
