package mihon.entry.interactions.reader.settings

import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingsProvider

interface BookDocumentReaderSettings : ViewerSettingsProvider {
    val themeModeSetting: ViewerSettingDefinition<BookDocumentReaderThemeMode>
    val textSizeSetting: ViewerSettingDefinition<Int>
    val showStatusBarSetting: ViewerSettingDefinition<Boolean>
    val showNavigationBarSetting: ViewerSettingDefinition<Boolean>
    val showTextSelectionMenuSetting: ViewerSettingDefinition<Boolean>
    val showReadingProgressSetting: ViewerSettingDefinition<Boolean>
    val readingProgressStyleSetting: ViewerSettingDefinition<BookDocumentReaderProgressStyle>

    companion object {
        const val SURFACE_ID = "builtin.book.document"
        const val DEFAULT_TEXT_SIZE_PERCENT = 100
        const val MIN_TEXT_SIZE_PERCENT = 80
        const val MAX_TEXT_SIZE_PERCENT = 200
        const val TEXT_SIZE_STEP_PERCENT = 10

        val TEXT_SIZE_RANGE = MIN_TEXT_SIZE_PERCENT..MAX_TEXT_SIZE_PERCENT

        fun isValidTextSizePercent(value: Int): Boolean = value in TEXT_SIZE_RANGE
    }
}
