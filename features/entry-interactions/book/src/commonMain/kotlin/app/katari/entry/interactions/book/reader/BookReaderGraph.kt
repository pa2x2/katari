package app.katari.entry.interactions.book.reader

import app.katari.core.platform.clipboard.TextClipboard
import app.katari.core.platform.link.LinkOpener
import app.katari.core.platform.power.PowerSource
import app.katari.entry.interactions.book.document.reader.font.BookDocumentReaderFontStore
import app.katari.entry.interactions.book.document.reader.settings.BookDocumentReaderSettingsProvider
import app.katari.entry.interactions.book.document.resource.BookDocumentResourceDecoding
import app.katari.entry.interactions.book.document.resource.BookRemoteResourceConsentPreferences
import app.katari.entry.interactions.book.navigation.BookChapterNavigationResolver
import app.katari.entry.interactions.book.reader.failure.BookReaderChapterHeadingLoader
import app.katari.entry.interactions.child.EntryChildListFeature
import app.katari.entry.interactions.media.handoff.EntryProgressOffers
import app.katari.entry.interactions.reader.image.export.ReaderImageExport
import app.katari.entry.interactions.runtime.usage.ReadingSessionReports
import app.katari.entry.interactions.source.EntryWebViewFeature
import app.katari.entry.interactions.state.EntryBookmarkFeature
import app.katari.entry.interactions.translation.EntryTranslationLanguagesFeature
import app.katari.entry.viewer.settings.ViewerSettingBinder
import app.katari.entry.viewer.settings.shared.ReaderSharedSettingsRegistry
import app.katari.entry.viewer.settings.ui.device.ViewerDeviceControls
import app.katari.translation.api.TranslationFeature
import app.katari.translation.api.host.TranslationHostActions
import app.katari.tts.api.TtsFeature
import app.katari.tts.api.host.TtsHostActions
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import tachiyomi.core.common.storage.StorageFileResolver
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository

/**
 * Graph entry point of the book reader for hosts the graph doesn't construct, such as Android's activities. The
 * reader's services are internal to this module, which a contributed graph interface can't expose, so the graph hands
 * out [BookReaderDependencies] instead.
 */
@ContributesTo(AppScope::class)
interface BookReaderGraph {
    val bookReaderDependencies: BookReaderDependencies
}

/**
 * Each service is created on first use, so opening a reader doesn't build the translation and speech engines it may
 * never need.
 */
@Inject
class BookReaderDependencies internal constructor(
    hostResolver: Lazy<BookReaderHostResolver>,
    sessionFactory: Lazy<BookReaderSessionFactory>,
    sessionRegistry: Lazy<BookReaderSessionRegistry>,
    chapterNavigationResolver: Lazy<BookChapterNavigationResolver>,
    settingsProvider: Lazy<BookDocumentReaderSettingsProvider>,
    viewerSettingBinder: Lazy<ViewerSettingBinder>,
    remoteResourceConsentPreferences: Lazy<BookRemoteResourceConsentPreferences>,
    getEntryWithChapters: Lazy<GetEntryWithChapters>,
    chapterGroupRepository: Lazy<EntryChapterGroupRepository>,
    childListFeature: Lazy<EntryChildListFeature>,
    bookmarkFeature: Lazy<EntryBookmarkFeature>,
    webViewFeature: Lazy<EntryWebViewFeature>,
    translationFeature: Lazy<TranslationFeature>,
    translationHostActions: Lazy<TranslationHostActions>,
    translationLanguagesFeature: Lazy<EntryTranslationLanguagesFeature>,
    ttsFeature: Lazy<TtsFeature>,
    ttsHostActions: Lazy<TtsHostActions>,
    chapterHeadingLoader: Lazy<BookReaderChapterHeadingLoader>,
    fontStore: Lazy<BookDocumentReaderFontStore>,
    textClipboard: Lazy<TextClipboard>,
    resourceDecoding: Lazy<BookDocumentResourceDecoding>,
    imageExport: Lazy<ReaderImageExport>,
    storageFiles: Lazy<StorageFileResolver>,
    powerSource: Lazy<PowerSource>,
    linkOpener: Lazy<LinkOpener>,
    sharedSettingsRegistry: Lazy<ReaderSharedSettingsRegistry>,
    deviceControls: Lazy<ViewerDeviceControls>,
    progressOffers: Lazy<EntryProgressOffers>,
    readingSessionReports: Lazy<ReadingSessionReports>,
) {
    internal val hostResolver by hostResolver
    internal val sessionFactory by sessionFactory
    internal val sessionRegistry by sessionRegistry
    internal val chapterNavigationResolver by chapterNavigationResolver
    internal val settingsProvider by settingsProvider
    internal val viewerSettingBinder by viewerSettingBinder
    internal val remoteResourceConsentPreferences by remoteResourceConsentPreferences
    internal val getEntryWithChapters by getEntryWithChapters
    internal val chapterGroupRepository by chapterGroupRepository
    internal val childListFeature by childListFeature
    internal val bookmarkFeature by bookmarkFeature
    internal val webViewFeature by webViewFeature
    internal val translationFeature by translationFeature
    internal val translationHostActions by translationHostActions
    internal val translationLanguagesFeature by translationLanguagesFeature
    internal val ttsFeature by ttsFeature
    internal val ttsHostActions by ttsHostActions
    internal val chapterHeadingLoader by chapterHeadingLoader
    internal val fontStore by fontStore
    internal val textClipboard by textClipboard
    internal val resourceDecoding by resourceDecoding
    internal val imageExport by imageExport
    internal val storageFiles by storageFiles
    internal val powerSource by powerSource
    internal val linkOpener by linkOpener
    internal val sharedSettingsRegistry by sharedSettingsRegistry
    internal val deviceControls by deviceControls
    internal val progressOffers by progressOffers
    internal val readingSessionReports by readingSessionReports
}
