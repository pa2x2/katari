package mihon.entry.interactions.settings

import mihon.entry.viewer.settings.ViewerSettingCodecs
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsCategory
import mihon.entry.viewer.settings.ViewerSettingsProvider
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ReadiumEpubSettingsProvider(
    preferenceStore: PreferenceStore,
) : ViewerSettingsProvider {
    override val id: String = PROVIDER_ID
    override val category: ViewerSettingsCategory = ViewerSettingsCategory.READER
    override val displayName: String = "EPUB reader — Readium"

    private val theme = preferenceStore.getString("book.epub.readium.theme", THEME_LIGHT)
    private val fontFamily = preferenceStore.getString("book.epub.readium.font_family", FONT_PUBLISHER)
    private val fontSize = preferenceStore.getInt("book.epub.readium.font_size_percent", 100)
    private val lineHeight = preferenceStore.getInt("book.epub.readium.line_height_percent", 120)
    private val pageMargins = preferenceStore.getInt("book.epub.readium.page_margins_percent", 100)
    private val publisherStyles = preferenceStore.getBoolean("book.epub.readium.publisher_styles", true)
    private val textAlignment = preferenceStore.getString("book.epub.readium.text_alignment", ALIGN_PUBLISHER)
    private val layoutMode = preferenceStore.getString("book.epub.readium.layout_mode", LAYOUT_PAGINATED)
    private val columnCount = preferenceStore.getString("book.epub.readium.column_count", COLUMNS_AUTO)
    private val textNormalization = preferenceStore.getBoolean("book.epub.readium.text_normalization", false)
    private val tapNavigation = preferenceStore.getBoolean("book.epub.readium.tap_navigation", false)
    private val showPageNumber = preferenceStore.getBoolean("book.epub.readium.show_page_number", true)

    val themeSetting = stringSetting(THEME_KEY, theme, SUPPORTED_THEMES)
    val fontFamilySetting = stringSetting(FONT_FAMILY_KEY, fontFamily, SUPPORTED_FONT_FAMILIES)
    val fontSizeSetting = intSetting(FONT_SIZE_KEY, fontSize, FONT_SIZE_RANGE)
    val lineHeightSetting = intSetting(LINE_HEIGHT_KEY, lineHeight, LINE_HEIGHT_RANGE)
    val pageMarginsSetting = intSetting(PAGE_MARGINS_KEY, pageMargins, PAGE_MARGINS_RANGE)
    val publisherStylesSetting = booleanSetting(PUBLISHER_STYLES_KEY, publisherStyles)
    val textAlignmentSetting = stringSetting(TEXT_ALIGNMENT_KEY, textAlignment, SUPPORTED_TEXT_ALIGNMENTS)
    val layoutModeSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, LAYOUT_MODE_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = LAYOUT_PAGINATED,
        profilePreference = layoutMode,
        codec = ViewerSettingCodecs.String,
        validate = SUPPORTED_LAYOUT_MODES::contains,
    )
    val columnCountSetting = stringSetting(COLUMN_COUNT_KEY, columnCount, SUPPORTED_COLUMN_COUNTS)
    val textNormalizationSetting = booleanSetting(TEXT_NORMALIZATION_KEY, textNormalization)
    val tapNavigationSetting = booleanSetting(TAP_NAVIGATION_KEY, tapNavigation)
    val showPageNumberSetting = booleanSetting(SHOW_PAGE_NUMBER_KEY, showPageNumber)

    override val settings: List<ViewerSettingDefinition<*>> = listOf(
        themeSetting,
        fontFamilySetting,
        fontSizeSetting,
        lineHeightSetting,
        pageMarginsSetting,
        publisherStylesSetting,
        textAlignmentSetting,
        layoutModeSetting,
        columnCountSetting,
        textNormalizationSetting,
        tapNavigationSetting,
        showPageNumberSetting,
    )

    private fun booleanSetting(key: String, preference: Preference<Boolean>) = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, key),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = preference.defaultValue(),
        profilePreference = preference,
        codec = ViewerSettingCodecs.Boolean,
    )

    private fun intSetting(key: String, preference: Preference<Int>, range: IntRange) = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, key),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = preference.defaultValue(),
        profilePreference = preference,
        codec = ViewerSettingCodecs.Int,
        validate = range::contains,
    )

    private fun stringSetting(
        key: String,
        preference: Preference<String>,
        supportedValues: Set<String>,
    ) = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, key),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = preference.defaultValue(),
        profilePreference = preference,
        codec = ViewerSettingCodecs.String,
        validate = supportedValues::contains,
    )

    companion object {
        const val PROVIDER_ID = "builtin.book.epub.readium"

        const val THEME_KEY = "theme"
        const val FONT_FAMILY_KEY = "font_family"
        const val FONT_SIZE_KEY = "font_size_percent"
        const val LINE_HEIGHT_KEY = "line_height_percent"
        const val PAGE_MARGINS_KEY = "page_margins_percent"
        const val PUBLISHER_STYLES_KEY = "publisher_styles"
        const val TEXT_ALIGNMENT_KEY = "text_alignment"
        const val LAYOUT_MODE_KEY = "layout_mode"
        const val COLUMN_COUNT_KEY = "column_count"
        const val TEXT_NORMALIZATION_KEY = "text_normalization"
        const val TAP_NAVIGATION_KEY = "tap_navigation"
        const val SHOW_PAGE_NUMBER_KEY = "show_page_number"

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SEPIA = "sepia"
        val SUPPORTED_THEMES = setOf(THEME_LIGHT, THEME_DARK, THEME_SEPIA)

        const val FONT_PUBLISHER = "publisher"
        const val FONT_SERIF = "serif"
        const val FONT_SANS_SERIF = "sans_serif"
        const val FONT_MONOSPACE = "monospace"
        const val FONT_OPEN_DYSLEXIC = "open_dyslexic"
        val SUPPORTED_FONT_FAMILIES = setOf(
            FONT_PUBLISHER,
            FONT_SERIF,
            FONT_SANS_SERIF,
            FONT_MONOSPACE,
            FONT_OPEN_DYSLEXIC,
        )

        const val ALIGN_PUBLISHER = "publisher"
        const val ALIGN_START = "start"
        const val ALIGN_JUSTIFY = "justify"
        const val ALIGN_LEFT = "left"
        const val ALIGN_RIGHT = "right"
        val SUPPORTED_TEXT_ALIGNMENTS = setOf(
            ALIGN_PUBLISHER,
            ALIGN_START,
            ALIGN_JUSTIFY,
            ALIGN_LEFT,
            ALIGN_RIGHT,
        )

        const val LAYOUT_PAGINATED = "paginated"
        const val LAYOUT_SCROLLING = "scrolling"
        val SUPPORTED_LAYOUT_MODES = setOf(LAYOUT_PAGINATED, LAYOUT_SCROLLING)

        const val COLUMNS_AUTO = "auto"
        const val COLUMNS_ONE = "one"
        const val COLUMNS_TWO = "two"
        val SUPPORTED_COLUMN_COUNTS = setOf(COLUMNS_AUTO, COLUMNS_ONE, COLUMNS_TWO)

        val FONT_SIZE_RANGE = 50..300
        val LINE_HEIGHT_RANGE = 100..200
        val PAGE_MARGINS_RANGE = 0..400
    }
}
