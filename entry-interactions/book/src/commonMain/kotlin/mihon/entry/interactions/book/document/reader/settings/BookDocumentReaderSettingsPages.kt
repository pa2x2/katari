package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.runtime.Composable
import mihon.entry.interactions.book.document.reader.font.BookDocumentReaderFonts
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap

/** The book reader's own settings tabs, after the shared one. */
@Composable
internal fun BookDocumentReaderAppearancePage(
    bindings: BookDocumentReaderSettingBindings,
    fonts: BookDocumentReaderFonts,
) {
    BookDocumentReaderThemeSettings(bindings)
    RowGroupGap()
    RowGroup {
        row { BookDocumentReaderTextSizeSettings(bindings.textSize) }
        row { BookDocumentReaderFontSettings(bindings.font, fonts) }
    }
    BookDocumentReaderTypographySettings(bindings)
}

/**
 * [progressStyleAvailable] is whether the reading mode offers progress styles; only scrolling does. [systemBars] is
 * whether the window has status and navigation bars the reader can show, which a desktop window doesn't, and
 * [selectionToolbar] whether selected text gets a floating toolbar whose system actions a setting can leave out.
 */
@Composable
internal fun BookDocumentReaderBehaviorPage(
    bindings: BookDocumentReaderSettingBindings,
    progressStyleAvailable: Boolean,
    systemBars: Boolean,
    selectionToolbar: Boolean,
) {
    BookDocumentReaderPagingSettings(bindings)
    BookDocumentReaderChapterTransitionSettings(bindings.chapterTransitionMode)
    RowGroup {
        row { BookDocumentReaderScreenAliveSettings(bindings.keepScreenAlive) }
        if (selectionToolbar) row { BookDocumentReaderTextSelectionMenuSettings(bindings.showTextSelectionMenu) }
        if (systemBars) row { BookDocumentReaderStatusBarSettings(bindings.showStatusBar) }
        row {
            BookDocumentReaderTimeAndBatterySettings(
                binding = bindings.showTimeAndBattery,
                statusBarBinding = bindings.showStatusBar.takeIf { systemBars },
            )
        }
        if (systemBars) row { BookDocumentReaderNavigationBarSettings(bindings.showNavigationBar) }
        row { BookDocumentReaderShowProgressSetting(bindings.showReadingProgress) }
    }
    BookDocumentReaderProgressStyleSettings(
        showProgressBinding = bindings.showReadingProgress,
        styleBinding = bindings.readingProgressStyle,
        available = progressStyleAvailable,
    )
}
