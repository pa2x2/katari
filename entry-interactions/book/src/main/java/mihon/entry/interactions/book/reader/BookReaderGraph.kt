package mihon.entry.interactions.book.reader

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderSettingsProvider
import mihon.entry.interactions.book.document.resource.BookRemoteResourceConsentPreferences
import mihon.entry.interactions.book.navigation.BookChapterNavigationResolver
import mihon.entry.interactions.book.reader.failure.BookReaderChapterHeadingLoader
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.source.EntryWebViewFeature
import mihon.entry.interactions.state.EntryBookmarkFeature
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import mihon.tts.api.TtsFeature
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository

/**
 * Graph entry point of the book reader activities, which Android constructs. The activities and most services they use
 * are internal to this module, which a contributed graph interface can't expose, so the graph hands out
 * [BookReaderDependencies] instead.
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
    chapterHeadingLoader: Lazy<BookReaderChapterHeadingLoader>,
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
    internal val chapterHeadingLoader by chapterHeadingLoader
}
