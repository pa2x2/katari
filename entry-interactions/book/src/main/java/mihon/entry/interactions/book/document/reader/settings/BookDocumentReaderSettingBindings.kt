package mihon.entry.interactions.book.document.reader.settings

import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationSettingsProvider
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.shared.ReaderSharedSettingId
import mihon.entry.viewer.settings.shared.StandardReaderSharedSettingIds

internal class BookDocumentReaderSettingBindings private constructor(
    val readingMode: ViewerSettingBinding<BookDocumentReadingMode>,
    val tapZones: ViewerSettingBinding<Int>,
    val tapInversion: ViewerSettingBinding<Int>,
    val animatePages: ViewerSettingBinding<Boolean>,
    val volumeKeys: ViewerSettingBinding<Boolean>,
    val invertVolumeKeys: ViewerSettingBinding<Boolean>,
    val themeMode: ViewerSettingBinding<BookDocumentReaderThemeMode>,
    val textSize: ViewerSettingBinding<Int>,
    val keepScreenAlive: ViewerSettingBinding<Boolean>,
    val showStatusBar: ViewerSettingBinding<Boolean>,
    val showNavigationBar: ViewerSettingBinding<Boolean>,
    val showTextSelectionMenu: ViewerSettingBinding<Boolean>,
    val showReadingProgress: ViewerSettingBinding<Boolean>,
    val readingProgressStyle: ViewerSettingBinding<BookDocumentReaderProgressStyle>,
    val sharedSettings: Map<ReaderSharedSettingId, ViewerSettingBinding<Boolean>>,
) {
    val prepareNextChapter: ViewerSettingBinding<Boolean>
        get() = sharedSettings.getValue(StandardReaderSharedSettingIds.NextChapterPreparation)

    val automaticTranslation: ViewerSettingBinding<Boolean>
        get() = sharedSettings.getValue(BookAutomaticTranslationSettingsProvider.AUTOMATIC_SELECTION_SETTING_ID)

    companion object {
        suspend fun create(
            provider: BookDocumentReaderSettingsProvider,
            binder: ViewerSettingBinder,
            entryId: Long,
        ): BookDocumentReaderSettingBindings {
            val entryBinder = binder.initializeEntry(entryId)
            return BookDocumentReaderSettingBindings(
                readingMode = entryBinder.bind(provider.readingModeSetting),
                tapZones = entryBinder.bind(provider.tapZonesSetting),
                tapInversion = entryBinder.bind(provider.tapInversionSetting),
                animatePages = entryBinder.bind(provider.animatePagesSetting),
                volumeKeys = entryBinder.bind(provider.volumeKeysSetting),
                invertVolumeKeys = entryBinder.bind(provider.invertVolumeKeysSetting),
                themeMode = entryBinder.bind(provider.themeModeSetting),
                textSize = entryBinder.bind(provider.textSizeSetting),
                keepScreenAlive = entryBinder.bind(provider.keepScreenAliveSetting),
                showStatusBar = entryBinder.bind(provider.showStatusBarSetting),
                showNavigationBar = entryBinder.bind(provider.showNavigationBarSetting),
                showTextSelectionMenu = entryBinder.bind(provider.showTextSelectionMenuSetting),
                showReadingProgress = entryBinder.bind(provider.showReadingProgressSetting),
                readingProgressStyle = entryBinder.bind(provider.readingProgressStyleSetting),
                sharedSettings = provider.sharedSettingDefinitions.mapValues { (_, definition) ->
                    entryBinder.bind(definition)
                },
            )
        }
    }
}
