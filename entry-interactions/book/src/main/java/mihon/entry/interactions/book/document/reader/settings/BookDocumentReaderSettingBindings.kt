package mihon.entry.interactions.book.document.reader.settings

import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationSettingsProvider
import mihon.entry.interactions.reader.settings.BookDocumentLineSpacing
import mihon.entry.interactions.reader.settings.BookDocumentMargins
import mihon.entry.interactions.reader.settings.BookDocumentPageLayout
import mihon.entry.interactions.reader.settings.BookDocumentReaderFont
import mihon.entry.interactions.reader.settings.BookDocumentReaderTheme
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.interactions.reader.settings.BookDocumentTextAlignment
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.shared.ReaderSharedSettingId
import mihon.entry.viewer.settings.shared.StandardReaderSharedSettingIds
import tachiyomi.core.common.preference.Preference

internal class BookDocumentReaderSettingBindings private constructor(
    val readingMode: ViewerSettingBinding<BookDocumentReadingMode>,
    val pageLayout: ViewerSettingBinding<BookDocumentPageLayout>,
    val tapZones: ViewerSettingBinding<Int>,
    val tapInversion: ViewerSettingBinding<Int>,
    val animatePages: ViewerSettingBinding<Boolean>,
    val volumeKeys: ViewerSettingBinding<Boolean>,
    val invertVolumeKeys: ViewerSettingBinding<Boolean>,
    val theme: ViewerSettingBinding<BookDocumentReaderTheme>,
    val savedColors: Preference<List<BookDocumentReaderTheme.Custom>>,
    val publisherStyles: ViewerSettingBinding<Boolean>,
    val textSize: ViewerSettingBinding<Int>,
    val font: ViewerSettingBinding<BookDocumentReaderFont>,
    val lineSpacing: ViewerSettingBinding<BookDocumentLineSpacing>,
    val margins: ViewerSettingBinding<BookDocumentMargins>,
    val textAlignment: ViewerSettingBinding<BookDocumentTextAlignment>,
    val keepScreenAlive: ViewerSettingBinding<Boolean>,
    val showStatusBar: ViewerSettingBinding<Boolean>,
    val showTimeAndBattery: ViewerSettingBinding<Boolean>,
    val showNavigationBar: ViewerSettingBinding<Boolean>,
    val showTextSelectionMenu: ViewerSettingBinding<Boolean>,
    val showReadingProgress: ViewerSettingBinding<Boolean>,
    val readingProgressStyle: ViewerSettingBinding<BookDocumentReaderProgressStyle>,
    val chapterTransitionMode: ViewerSettingBinding<ChapterTransitionMode>,
    val sharedSettings: Map<ReaderSharedSettingId, ViewerSettingBinding<Boolean>>,
) {
    val prepareNextChapter: ViewerSettingBinding<Boolean>
        get() = sharedSettings.getValue(StandardReaderSharedSettingIds.NextChapterPreparation)

    val automaticTranslation: ViewerSettingBinding<Boolean>
        get() = sharedSettings.getValue(BookAutomaticTranslationSettingsProvider.AUTOMATIC_SELECTION_SETTING_ID)

    // Every setting of the reader's own tabs; the shared ones on the Common tab reset there.
    private val processorSettings: List<ViewerSettingBinding<*>> = listOf(
        readingMode,
        pageLayout,
        tapZones,
        tapInversion,
        animatePages,
        volumeKeys,
        invertVolumeKeys,
        theme,
        publisherStyles,
        textSize,
        font,
        lineSpacing,
        margins,
        textAlignment,
        keepScreenAlive,
        showStatusBar,
        showTimeAndBattery,
        showNavigationBar,
        showTextSelectionMenu,
        showReadingProgress,
        readingProgressStyle,
        chapterTransitionMode,
    )

    /** Clears this book's overrides of the reader's own settings, leaving the profile's values. */
    suspend fun clearProcessorEntryOverrides() {
        processorSettings.forEach { it.clearEntryOverride() }
    }

    companion object {
        suspend fun create(
            provider: BookDocumentReaderSettingsProvider,
            binder: ViewerSettingBinder,
            entryId: Long,
        ): BookDocumentReaderSettingBindings {
            val entryBinder = binder.initializeEntry(entryId)
            return BookDocumentReaderSettingBindings(
                readingMode = entryBinder.bind(provider.readingModeSetting),
                pageLayout = entryBinder.bind(provider.pageLayoutSetting),
                tapZones = entryBinder.bind(provider.tapZonesSetting),
                tapInversion = entryBinder.bind(provider.tapInversionSetting),
                animatePages = entryBinder.bind(provider.animatePagesSetting),
                volumeKeys = entryBinder.bind(provider.volumeKeysSetting),
                invertVolumeKeys = entryBinder.bind(provider.invertVolumeKeysSetting),
                theme = entryBinder.bind(provider.themeSetting),
                savedColors = provider.savedColors,
                publisherStyles = entryBinder.bind(provider.publisherStylesSetting),
                textSize = entryBinder.bind(provider.textSizeSetting),
                font = entryBinder.bind(provider.fontSetting),
                lineSpacing = entryBinder.bind(provider.lineSpacingSetting),
                margins = entryBinder.bind(provider.marginsSetting),
                textAlignment = entryBinder.bind(provider.textAlignmentSetting),
                keepScreenAlive = entryBinder.bind(provider.keepScreenAliveSetting),
                showStatusBar = entryBinder.bind(provider.showStatusBarSetting),
                showTimeAndBattery = entryBinder.bind(provider.showTimeAndBatterySetting),
                showNavigationBar = entryBinder.bind(provider.showNavigationBarSetting),
                showTextSelectionMenu = entryBinder.bind(provider.showTextSelectionMenuSetting),
                showReadingProgress = entryBinder.bind(provider.showReadingProgressSetting),
                readingProgressStyle = entryBinder.bind(provider.readingProgressStyleSetting),
                chapterTransitionMode = entryBinder.bind(provider.chapterTransitionModeSetting),
                sharedSettings = provider.sharedSettingDefinitions.mapValues { (_, definition) ->
                    entryBinder.bind(definition)
                },
            )
        }
    }
}
