@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package mihon.entry.interactions.book.epub

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import mihon.entry.interactions.settings.ReadiumEpubSettingsProvider
import mihon.entry.viewer.settings.ViewerSettingBinder
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme

internal class ReadiumEpubSettingsBinding(
    private val provider: ReadiumEpubSettingsProvider,
    private val binder: ViewerSettingBinder,
    private val entryId: Long,
) {
    private val theme = binder.bind(provider.themeSetting)
    private val fontFamily = binder.bind(provider.fontFamilySetting)
    private val fontSize = binder.bind(provider.fontSizeSetting)
    private val lineHeight = binder.bind(provider.lineHeightSetting)
    private val pageMargins = binder.bind(provider.pageMarginsSetting)
    private val publisherStyles = binder.bind(provider.publisherStylesSetting)
    private val textAlignment = binder.bind(provider.textAlignmentSetting)
    private val layoutMode = binder.bind(provider.layoutModeSetting, entryId)
    private val columnCount = binder.bind(provider.columnCountSetting)
    private val textNormalization = binder.bind(provider.textNormalizationSetting)

    private val appearance = combine(
        theme.state,
        fontFamily.state,
        fontSize.state,
        pageMargins.state,
        textNormalization.state,
    ) { theme, fontFamily, fontSize, pageMargins, textNormalization ->
        AppearanceValues(
            theme = theme.effectiveValue,
            fontFamily = fontFamily.effectiveValue,
            fontSizePercent = fontSize.effectiveValue,
            pageMarginsPercent = pageMargins.effectiveValue,
            textNormalization = textNormalization.effectiveValue,
        )
    }
    private val textLayout = combine(
        lineHeight.state,
        publisherStyles.state,
        textAlignment.state,
    ) { lineHeight, publisherStyles, textAlignment ->
        TextLayoutValues(
            lineHeightPercent = lineHeight.effectiveValue,
            publisherStyles = publisherStyles.effectiveValue,
            textAlignment = textAlignment.effectiveValue,
        )
    }
    private val pageLayout = combine(layoutMode.state, columnCount.state) { layoutMode, columnCount ->
        PageLayoutValues(
            layoutMode = layoutMode.effectiveValue,
            columnCount = columnCount.effectiveValue,
        )
    }

    val changes: Flow<EpubPreferences> = combine(appearance, textLayout, pageLayout, ::toReadiumPreferences)
        .drop(1)
        .distinctUntilChanged()

    suspend fun initialPreferences(): EpubPreferences {
        return toReadiumPreferences(
            AppearanceValues(
                theme = binder.resolve(provider.themeSetting).effectiveValue,
                fontFamily = binder.resolve(provider.fontFamilySetting).effectiveValue,
                fontSizePercent = binder.resolve(provider.fontSizeSetting).effectiveValue,
                pageMarginsPercent = binder.resolve(provider.pageMarginsSetting).effectiveValue,
                textNormalization = binder.resolve(provider.textNormalizationSetting).effectiveValue,
            ),
            TextLayoutValues(
                lineHeightPercent = binder.resolve(provider.lineHeightSetting).effectiveValue,
                publisherStyles = binder.resolve(provider.publisherStylesSetting).effectiveValue,
                textAlignment = binder.resolve(provider.textAlignmentSetting).effectiveValue,
            ),
            PageLayoutValues(
                layoutMode = binder.resolve(provider.layoutModeSetting, entryId).effectiveValue,
                columnCount = binder.resolve(provider.columnCountSetting).effectiveValue,
            ),
        )
    }
}

internal data class AppearanceValues(
    val theme: String,
    val fontFamily: String,
    val fontSizePercent: Int,
    val pageMarginsPercent: Int,
    val textNormalization: Boolean,
)

internal data class TextLayoutValues(
    val lineHeightPercent: Int,
    val publisherStyles: Boolean,
    val textAlignment: String,
)

internal data class PageLayoutValues(
    val layoutMode: String,
    val columnCount: String,
)

internal fun toReadiumPreferences(
    appearance: AppearanceValues,
    textLayout: TextLayoutValues,
    pageLayout: PageLayoutValues,
): EpubPreferences = EpubPreferences(
    theme = when (appearance.theme) {
        ReadiumEpubSettingsProvider.THEME_DARK -> Theme.DARK
        ReadiumEpubSettingsProvider.THEME_SEPIA -> Theme.SEPIA
        else -> Theme.LIGHT
    },
    fontFamily = when (appearance.fontFamily) {
        ReadiumEpubSettingsProvider.FONT_SERIF -> FontFamily.SERIF
        ReadiumEpubSettingsProvider.FONT_SANS_SERIF -> FontFamily.SANS_SERIF
        ReadiumEpubSettingsProvider.FONT_MONOSPACE -> FontFamily.MONOSPACE
        ReadiumEpubSettingsProvider.FONT_OPEN_DYSLEXIC -> FontFamily.OPEN_DYSLEXIC
        else -> null
    },
    fontSize = appearance.fontSizePercent / 100.0,
    lineHeight = textLayout.lineHeightPercent / 100.0,
    pageMargins = appearance.pageMarginsPercent / 100.0,
    publisherStyles = textLayout.publisherStyles,
    textAlign = when (textLayout.textAlignment) {
        ReadiumEpubSettingsProvider.ALIGN_START -> TextAlign.START
        ReadiumEpubSettingsProvider.ALIGN_JUSTIFY -> TextAlign.JUSTIFY
        ReadiumEpubSettingsProvider.ALIGN_LEFT -> TextAlign.LEFT
        ReadiumEpubSettingsProvider.ALIGN_RIGHT -> TextAlign.RIGHT
        else -> null
    },
    scroll = pageLayout.layoutMode == ReadiumEpubSettingsProvider.LAYOUT_SCROLLING,
    columnCount = when (pageLayout.columnCount) {
        ReadiumEpubSettingsProvider.COLUMNS_ONE -> ColumnCount.ONE
        ReadiumEpubSettingsProvider.COLUMNS_TWO -> ColumnCount.TWO
        else -> ColumnCount.AUTO
    },
    textNormalization = appearance.textNormalization,
)
