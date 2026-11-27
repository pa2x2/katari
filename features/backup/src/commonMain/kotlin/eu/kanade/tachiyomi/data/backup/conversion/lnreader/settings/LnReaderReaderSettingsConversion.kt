package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import app.katari.entry.interactions.reader.settings.BookDocumentLineSpacing
import app.katari.entry.interactions.reader.settings.BookDocumentMargins
import app.katari.entry.interactions.reader.settings.BookDocumentReaderFont
import app.katari.entry.interactions.reader.settings.BookDocumentReaderSettings
import app.katari.entry.interactions.reader.settings.BookDocumentReaderTheme
import app.katari.entry.interactions.reader.settings.BookDocumentReadingMode
import app.katari.entry.interactions.reader.settings.BookDocumentTextAlignment
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import tachiyomi.i18n.*
import kotlin.math.abs
import kotlin.math.roundToInt

/** LNReader's reader behaviour as the book reader's, which reads its novels in Katari. */
internal fun LnReaderSettings.convertChapterGeneralSettings(settings: LnReaderChapterGeneralSettings) {
    settings.keepScreenOn?.let { write(KEEP_SCREEN_ALIVE_KEY, BooleanPreferenceValue(it)) }
    // LNReader's fullscreen hid both bars; Katari has a switch for each
    settings.fullScreenMode?.let { fullScreen ->
        write(SHOW_STATUS_BAR_KEY, BooleanPreferenceValue(!fullScreen))
        write(SHOW_NAVIGATION_BAR_KEY, BooleanPreferenceValue(!fullScreen))
    }
    // LNReader's pages turn left to right; scrolling is Katari's default
    if (settings.pageReader == true) {
        write(READING_MODE_KEY, StringPreferenceValue(BookDocumentReadingMode.PAGED_LTR.name))
    }
    settings.showScrollPercentage?.let { write(SHOW_READING_PROGRESS_KEY, BooleanPreferenceValue(it)) }
    settings.useVolumeButtons?.let { write(VOLUME_KEYS_KEY, BooleanPreferenceValue(it)) }
    settings.showBatteryAndTime?.let { write(SHOW_TIME_AND_BATTERY_KEY, BooleanPreferenceValue(it)) }
    leaveBehindHeld(
        settings.swipeGestures to LnReaderSettingLabels.SWIPE_BETWEEN_CHAPTERS,
        settings.volumeButtonsOffset to LnReaderSettingLabels.VOLUME_BUTTON_OFFSET,
        settings.autoScroll to LnReaderSettingLabels.AUTO_SCROLL,
        settings.autoScrollInterval to LnReaderSettingLabels.AUTO_SCROLL_INTERVAL,
        settings.autoScrollOffset to LnReaderSettingLabels.AUTO_SCROLL_OFFSET,
        settings.verticalSeekbar to LnReaderSettingLabels.VERTICAL_SEEKBAR,
        settings.tapToScroll to LnReaderSettingLabels.TAP_TO_SCROLL,
        settings.bionicReading to LnReaderSettingLabels.BIONIC_READING,
        settings.removeExtraParagraphSpacing to LnReaderSettingLabels.REMOVE_EXTRA_SPACING,
        settings.ttsEnabled to LnReaderSettingLabels.ENABLE_TTS,
    )
}

/**
 * LNReader's page colours, text size, spacing, alignment and font as the book reader's. Its free values snap to the
 * nearest of Katari's steps (D082), and its fonts map by kind (D083).
 */
internal fun LnReaderSettings.convertReaderLook(settings: LnReaderChapterReaderSettings) {
    convertColors(settings)
    settings.textSize?.let { pixels ->
        val percent = (pixels / BASE_TEXT_SIZE_PX * 100 / TEXT_SIZE_STEP).roundToInt() * TEXT_SIZE_STEP
        write(
            TEXT_SIZE_PERCENT_KEY,
            IntPreferenceValue(percent.coerceIn(BookDocumentReaderSettings.TEXT_SIZE_RANGE)),
        )
    }
    settings.lineHeight?.let { height ->
        // In tenths, so a height halfway between two steps snaps to the smaller every time
        val tenths = (height * 10).roundToInt()
        val spacing = LINE_SPACINGS.minBy { (_, stepTenths) -> abs(stepTenths - tenths) }.first
        write(LINE_SPACING_KEY, StringPreferenceValue(spacing.name))
    }
    settings.padding?.let { pixels ->
        val margins = MARGINS.minBy { (_, inset) -> abs(inset - pixels) }.first
        write(MARGINS_KEY, StringPreferenceValue(margins.name))
    }
    settings.textAlign?.let(::convertTextAlignment)
    settings.fontFamily?.let(::convertFont)
}

private fun LnReaderSettings.convertColors(settings: LnReaderChapterReaderSettings) {
    if (settings.theme != null || settings.textColor != null) {
        // LNReader fills in its default for the colour the stored settings lack
        val colors = LnReaderReaderColors(
            settings.theme ?: DEFAULT_COLORS.backgroundColor,
            settings.textColor ?: DEFAULT_COLORS.textColor,
        )
        val theme = colors.toKatariTheme()
        if (theme == null) {
            leaveBehind(Item(Name.Setting(THEME_KEY), MR.strings.backup_conversion_lnreader_unreadable))
        } else {
            write(THEME_KEY, StringPreferenceValue(theme.id))
        }
    }
    settings.customThemes?.takeIf { it.isNotEmpty() }?.let { themes ->
        val saved = themes.mapNotNull { it.toKatariTheme() }.distinct()
        if (saved.size < themes.size) {
            leaveBehind(Item(Name.Setting(SAVED_COLORS_KEY), MR.strings.backup_conversion_lnreader_unreadable))
        }
        if (saved.isNotEmpty()) {
            write(SAVED_COLORS_KEY, StringPreferenceValue(BookDocumentReaderTheme.encodeCustomList(saved)))
        }
    }
}

// Katari aligns body text to the start or justifies it, else keeps the book's alignment
private fun LnReaderSettings.convertTextAlignment(align: String) {
    when (align) {
        "left" -> write(TEXT_ALIGNMENT_KEY, StringPreferenceValue(BookDocumentTextAlignment.LEFT.name))
        "justify" -> write(TEXT_ALIGNMENT_KEY, StringPreferenceValue(BookDocumentTextAlignment.JUSTIFY.name))
        "center", "right" ->
            leaveBehind(Item(Name.Setting(TEXT_ALIGNMENT_KEY), MR.strings.backup_conversion_lnreader_text_alignment))
    }
}

private fun LnReaderSettings.convertFont(family: String) {
    when (family) {
        OPEN_DYSLEXIC -> leaveBehind(
            LnReaderSettingLabels.FONT_STYLE,
            MR.strings.backup_conversion_lnreader_font_missing,
        )
        else -> {
            val font = FONTS[family]
            if (font == null) {
                leaveBehind(Item(Name.Setting(FONT_KEY), MR.strings.backup_conversion_lnreader_unreadable))
            } else {
                write(FONT_KEY, StringPreferenceValue(font.id))
            }
        }
    }
}

// BookDocumentReaderPreferences' keys
private const val READING_MODE_KEY = "book_document_reader_reading_mode"
private const val VOLUME_KEYS_KEY = "book_document_reader_volume_keys"
private const val THEME_KEY = "book_document_reader_theme_mode"
private const val SAVED_COLORS_KEY = "book_document_reader_saved_colors"
private const val TEXT_SIZE_PERCENT_KEY = "book_document_reader_text_size_percent"
private const val FONT_KEY = "book_document_reader_font"
private const val LINE_SPACING_KEY = "book_document_reader_line_spacing"
private const val MARGINS_KEY = "book_document_reader_margins"
private const val TEXT_ALIGNMENT_KEY = "book_document_reader_text_alignment"
private const val KEEP_SCREEN_ALIVE_KEY = "book_document_reader_keep_screen_alive"
private const val SHOW_STATUS_BAR_KEY = "book_document_reader_show_status_bar"
private const val SHOW_TIME_AND_BATTERY_KEY = "book_document_reader_show_time_and_battery"
private const val SHOW_NAVIGATION_BAR_KEY = "book_document_reader_show_navigation_bar"
private const val SHOW_READING_PROGRESS_KEY = "book_document_reader_show_reading_progress"

// Katari's book text is sized as a share of 16 sp, which LNReader's 16 px default matches
private const val BASE_TEXT_SIZE_PX = 16.0
private const val TEXT_SIZE_STEP = BookDocumentReaderSettings.TEXT_SIZE_STEP_PERCENT

// LNReader's default theme, initialChapterReaderSettings in src/hooks/persisted/useSettings.ts
private val DEFAULT_COLORS = LnReaderReaderColors(backgroundColor = "#292832", textColor = "#CCCCCC")

// Each step's line height in tenths, from BookDocumentTypography
private val LINE_SPACINGS = listOf(
    BookDocumentLineSpacing.TIGHT to 13,
    BookDocumentLineSpacing.NORMAL to 15,
    BookDocumentLineSpacing.LOOSE to 18,
)

// Each step's side inset in dp, from ReadingMetrics, against LNReader's side padding in px; ties go to the narrower
private val MARGINS = listOf(
    BookDocumentMargins.NARROW to 8.0,
    BookDocumentMargins.BOOK to 20.0,
    BookDocumentMargins.NORMAL to 28.0,
    BookDocumentMargins.WIDE to 48.0,
)

private const val OPEN_DYSLEXIC = "OpenDyslexic3-Regular"

// readerFonts of src/utils/constants/readerConstants.ts by kind; empty is the chapter's own
private val FONTS = mapOf(
    "" to BookDocumentReaderFont.Book,
    "lora" to BookDocumentReaderFont.Serif,
    "arbutus-slab" to BookDocumentReaderFont.Serif,
    "domine" to BookDocumentReaderFont.Serif,
    "pt-serif" to BookDocumentReaderFont.Serif,
    "nunito" to BookDocumentReaderFont.SansSerif,
    "noto-sans" to BookDocumentReaderFont.SansSerif,
    "open-sans" to BookDocumentReaderFont.SansSerif,
    "lato" to BookDocumentReaderFont.SansSerif,
)
