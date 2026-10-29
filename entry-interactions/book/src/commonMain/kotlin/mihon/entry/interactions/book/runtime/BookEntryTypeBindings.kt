package mihon.entry.interactions.book.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.core.platform.storage.AppPrivateDirectories
import mihon.entry.interactions.book.child.BookChildListProcessor
import mihon.entry.interactions.book.document.reader.BookDocumentReaderProcessor
import mihon.entry.interactions.book.document.reader.font.BookDocumentReaderFontStore
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderPreferences
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderSettingsProvider
import mihon.entry.interactions.book.document.resource.BookRemoteResourceConsentPreferences
import mihon.entry.interactions.book.library.BookLibraryProgressProvider
import mihon.entry.interactions.book.library.BookOutsideReleasePeriodFilterProvider
import mihon.entry.interactions.book.migration.BookMigrationProvider
import mihon.entry.interactions.book.presentation.BookEntryTypePresentationProvider
import mihon.entry.interactions.book.processor.BookReaderProcessorPreferences
import mihon.entry.interactions.book.processor.BookReaderProcessorRegistry
import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationPreferences
import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationSettingsProvider
import mihon.entry.interactions.book.state.BookConsumptionProcessor
import mihon.entry.interactions.book.state.BookProgressProcessor
import mihon.entry.interactions.book.state.BookTranslationLanguagesProvider
import mihon.entry.interactions.book.statistics.BookEntryStatisticsProvider
import mihon.entry.interactions.child.ChapterScanlatorFilterProcessor
import mihon.entry.interactions.library.EntryLibraryProgressProvider
import mihon.entry.interactions.media.DefaultEntryViewerSettingsProvider
import mihon.entry.interactions.media.EntryViewerSettingsProvider
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.interactions.reader.settings.BookDocumentReaderFontLibrary
import mihon.entry.interactions.reader.settings.BookDocumentReaderSettings
import mihon.entry.interactions.runtime.EntryChildListProcessor
import mihon.entry.interactions.runtime.EntryChildProgressProcessor
import mihon.entry.interactions.runtime.EntryOutsideReleasePeriodFilterProvider
import mihon.entry.interactions.runtime.EntryScanlatorFilterProcessor
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.runtime.EntryTypeKey
import mihon.entry.interactions.runtime.EntryTypePresentationProvider
import mihon.entry.interactions.runtime.ReaderSettingsSurfaceCapabilities
import mihon.entry.interactions.state.EntryConsumptionProcessor
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.state.EntryProgressProcessor
import mihon.entry.interactions.state.EntryTranslationLanguagesProvider
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsProvider
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.host.TranslationSettingsDestination
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository

/**
 * What book contributes on every platform: reading state, the chapter list, library progress, labels, statistics,
 * migration, the reader's settings and preferences, its automatic translation setting, the readers' settings surfaces
 * and the imported fonts. The readers,
 * downloads, publication preparation and what needs them, such as the media session events only a reader sends, are
 * contributed by the Android part, `BookEntryInteractionProviderBindings` and `BookEntryInteractionBindings`.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object BookEntryTypeBindings {
    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    @SingleIn(AppScope::class)
    internal fun consumption(
        entryProgressRepository: EntryProgressRepository,
        entryChapterRepository: EntryChapterRepository,
    ): EntryConsumptionProcessor = BookConsumptionProcessor(entryProgressRepository, entryChapterRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    @SingleIn(AppScope::class)
    internal fun progress(
        entryProgressRepository: EntryProgressRepository,
        entryChapterRepository: EntryChapterRepository,
    ): EntryProgressProcessor = BookProgressProcessor(entryProgressRepository, entryChapterRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun migration(): EntryMigrationProvider = BookMigrationProvider()

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun translationLanguages(): EntryTranslationLanguagesProvider = BookTranslationLanguagesProvider

    @Provides
    @SingleIn(AppScope::class)
    internal fun childListProcessor(entryProgressRepository: EntryProgressRepository): BookChildListProcessor =
        BookChildListProcessor(entryProgressRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun childList(processor: BookChildListProcessor): EntryChildListProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun scanlatorFilter(): EntryScanlatorFilterProcessor = ChapterScanlatorFilterProcessor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun childProgress(processor: BookChildListProcessor): EntryChildProgressProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    @SingleIn(AppScope::class)
    internal fun libraryProgress(entryProgressRepository: EntryProgressRepository): EntryLibraryProgressProvider =
        BookLibraryProgressProvider(entryProgressRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun outsideReleasePeriodFilter(): EntryOutsideReleasePeriodFilterProvider =
        BookOutsideReleasePeriodFilterProvider()

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun typePresentation(): EntryTypePresentationProvider = BookEntryTypePresentationProvider

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun statistics(): EntryStatisticsProvider = BookEntryStatisticsProvider

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    @SingleIn(AppScope::class)
    internal fun viewerSettings(provider: BookDocumentReaderSettingsProvider): EntryViewerSettingsProvider =
        DefaultEntryViewerSettingsProvider(surfaces = listOf(provider))

    @Provides
    @SingleIn(AppScope::class)
    internal fun readerProcessorRegistry(): BookReaderProcessorRegistry = BookReaderProcessorRegistry(
        processors = listOf(BookDocumentReaderProcessor()),
    )

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
    internal fun automaticTranslationSettingsProvider(
        processorRegistry: BookReaderProcessorRegistry,
        preferences: BookAutomaticTranslationPreferences,
        translationHostActions: TranslationHostActions,
        translationSettings: TranslationSettingsDestination,
    ): BookAutomaticTranslationSettingsProvider = BookAutomaticTranslationSettingsProvider(
        processorRegistry = processorRegistry,
        preferences = preferences,
        translationHostActions = translationHostActions,
        translationSettings = translationSettings,
    )

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
    internal fun documentReaderFontStore(directories: AppPrivateDirectories): BookDocumentReaderFontStore =
        BookDocumentReaderFontStore(directories)

    @Provides
    internal fun documentReaderFontLibrary(store: BookDocumentReaderFontStore): BookDocumentReaderFontLibrary = store
}
