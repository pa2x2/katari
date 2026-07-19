package mihon.entry.interactions.settings

import mihon.entry.viewer.settings.ViewerSettingCodecs
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsCategory
import mihon.entry.viewer.settings.ViewerSettingsProvider
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class HtmlProseSettingsProvider(
    preferenceStore: PreferenceStore,
) : ViewerSettingsProvider {
    override val id: String = PROVIDER_ID
    override val category: ViewerSettingsCategory = ViewerSettingsCategory.READER
    override val displayName: String = "Web prose reader"

    private val theme = preferenceStore.getString("book.prose.html.theme", THEME_SYSTEM)
    private val fontFamily = preferenceStore.getString("book.prose.html.font_family", FONT_SERIF)
    private val fontSize = preferenceStore.getInt("book.prose.html.font_size_percent", 100)
    private val lineHeight = preferenceStore.getInt("book.prose.html.line_height_percent", 170)
    private val pageMargins = preferenceStore.getInt("book.prose.html.page_margins_percent", 100)
    private val paragraphSpacing = preferenceStore.getInt("book.prose.html.paragraph_spacing_percent", 100)
    private val textAlignment = preferenceStore.getString("book.prose.html.text_alignment", ALIGN_START)
    private val layoutMode = preferenceStore.getString("book.prose.html.layout_mode", LAYOUT_PAGINATED)
    private val tapNavigation = preferenceStore.getBoolean("book.prose.html.tap_navigation", false)
    private val showProgress = preferenceStore.getBoolean("book.prose.html.show_progress", true)
    private val drawUnderCutout = preferenceStore.getBoolean("book.prose.html.draw_under_cutout", true)

    val themeSetting = stringSetting(THEME_KEY, theme, SUPPORTED_THEMES)
    val fontFamilySetting = stringSetting(FONT_FAMILY_KEY, fontFamily, SUPPORTED_FONT_FAMILIES)
    val fontSizeSetting = intSetting(FONT_SIZE_KEY, fontSize, FONT_SIZE_RANGE)
    val lineHeightSetting = intSetting(LINE_HEIGHT_KEY, lineHeight, LINE_HEIGHT_RANGE)
    val pageMarginsSetting = intSetting(PAGE_MARGINS_KEY, pageMargins, PAGE_MARGINS_RANGE)
    val paragraphSpacingSetting = intSetting(PARAGRAPH_SPACING_KEY, paragraphSpacing, PARAGRAPH_SPACING_RANGE)
    val textAlignmentSetting = stringSetting(TEXT_ALIGNMENT_KEY, textAlignment, SUPPORTED_TEXT_ALIGNMENTS)
    val layoutModeSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, LAYOUT_MODE_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = LAYOUT_PAGINATED,
        profilePreference = layoutMode,
        codec = ViewerSettingCodecs.String,
        validate = SUPPORTED_LAYOUT_MODES::contains,
    )
    val tapNavigationSetting = booleanSetting(TAP_NAVIGATION_KEY, tapNavigation)
    val showProgressSetting = booleanSetting(SHOW_PROGRESS_KEY, showProgress)
    val drawUnderCutoutSetting = booleanSetting(DRAW_UNDER_CUTOUT_KEY, drawUnderCutout)

    override val settings: List<ViewerSettingDefinition<*>> = listOf(
        themeSetting,
        fontFamilySetting,
        fontSizeSetting,
        lineHeightSetting,
        pageMarginsSetting,
        paragraphSpacingSetting,
        textAlignmentSetting,
        layoutModeSetting,
        tapNavigationSetting,
        showProgressSetting,
        drawUnderCutoutSetting,
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
        const val PROVIDER_ID = "builtin.book.prose.html"

        const val THEME_KEY = "theme"
        const val FONT_FAMILY_KEY = "font_family"
        const val FONT_SIZE_KEY = "font_size_percent"
        const val LINE_HEIGHT_KEY = "line_height_percent"
        const val PAGE_MARGINS_KEY = "page_margins_percent"
        const val PARAGRAPH_SPACING_KEY = "paragraph_spacing_percent"
        const val TEXT_ALIGNMENT_KEY = "text_alignment"
        const val LAYOUT_MODE_KEY = "layout_mode"
        const val TAP_NAVIGATION_KEY = "tap_navigation"
        const val SHOW_PROGRESS_KEY = "show_progress"
        const val DRAW_UNDER_CUTOUT_KEY = "draw_under_cutout"

        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SEPIA = "sepia"
        const val THEME_BLACK = "black"
        val SUPPORTED_THEMES = setOf(THEME_SYSTEM, THEME_LIGHT, THEME_DARK, THEME_SEPIA, THEME_BLACK)

        const val FONT_SERIF = "serif"
        const val FONT_SANS_SERIF = "sans_serif"
        const val FONT_MONOSPACE = "monospace"
        val SUPPORTED_FONT_FAMILIES = setOf(FONT_SERIF, FONT_SANS_SERIF, FONT_MONOSPACE)

        const val ALIGN_START = "start"
        const val ALIGN_JUSTIFY = "justify"
        const val ALIGN_LEFT = "left"
        const val ALIGN_RIGHT = "right"
        val SUPPORTED_TEXT_ALIGNMENTS = setOf(ALIGN_START, ALIGN_JUSTIFY, ALIGN_LEFT, ALIGN_RIGHT)

        const val LAYOUT_PAGINATED = "paginated"
        const val LAYOUT_SCROLLING = "scrolling"
        val SUPPORTED_LAYOUT_MODES = setOf(LAYOUT_PAGINATED, LAYOUT_SCROLLING)

        val FONT_SIZE_RANGE = 70..200
        val LINE_HEIGHT_RANGE = 100..220
        val PAGE_MARGINS_RANGE = 0..200
        val PARAGRAPH_SPACING_RANGE = 0..200
    }
}
