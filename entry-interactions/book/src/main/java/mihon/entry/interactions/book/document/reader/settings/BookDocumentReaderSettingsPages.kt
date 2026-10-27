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

/** [progressStyleAvailable] is whether the reading mode offers progress styles; only scrolling does. */
@Composable
internal fun BookDocumentReaderBehaviorPage(
    bindings: BookDocumentReaderSettingBindings,
    progressStyleAvailable: Boolean,
) {
    BookDocumentReaderPagingSettings(bindings)
    BookDocumentReaderChapterTransitionSettings(bindings.chapterTransitionMode)
    RowGroup {
        row { BookDocumentReaderScreenAliveSettings(bindings.keepScreenAlive) }
        row { BookDocumentReaderTextSelectionMenuSettings(bindings.showTextSelectionMenu) }
        row { BookDocumentReaderStatusBarSettings(bindings.showStatusBar) }
        row { BookDocumentReaderTimeAndBatterySettings(bindings.showTimeAndBattery, bindings.showStatusBar) }
        row { BookDocumentReaderNavigationBarSettings(bindings.showNavigationBar) }
        row { BookDocumentReaderShowProgressSetting(bindings.showReadingProgress) }
    }
    BookDocumentReaderProgressStyleSettings(
        showProgressBinding = bindings.showReadingProgress,
        styleBinding = bindings.readingProgressStyle,
        available = progressStyleAvailable,
    )
}
