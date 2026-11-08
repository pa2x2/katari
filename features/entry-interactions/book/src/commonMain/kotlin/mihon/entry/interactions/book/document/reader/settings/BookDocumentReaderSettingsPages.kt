package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.runtime.Composable
import mihon.entry.interactions.book.document.reader.font.BookDocumentReaderFonts
import mihon.entry.viewer.settings.ui.device.ViewerDeviceControls
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
 * [progressStyleAvailable] is whether the reading mode offers progress styles; only scrolling does. Rows for what
 * [deviceControls] says the platform lacks are left out.
 */
@Composable
internal fun BookDocumentReaderBehaviorPage(
    bindings: BookDocumentReaderSettingBindings,
    progressStyleAvailable: Boolean,
    deviceControls: ViewerDeviceControls,
) {
    BookDocumentReaderPagingSettings(bindings, volumeKeys = deviceControls.volumeKeys)
    BookDocumentReaderChapterTransitionSettings(bindings.chapterTransitionMode)
    RowGroup {
        row { BookDocumentReaderScreenAliveSettings(bindings.keepScreenAlive) }
        if (deviceControls.textSelectionToolbar) {
            row {
                BookDocumentReaderTextSelectionMenuSettings(bindings.showTextSelectionMenu)
            }
        }
        if (deviceControls.systemBars) row { BookDocumentReaderStatusBarSettings(bindings.showStatusBar) }
        row {
            BookDocumentReaderTimeAndBatterySettings(
                binding = bindings.showTimeAndBattery,
                statusBarBinding = bindings.showStatusBar.takeIf { deviceControls.systemBars },
            )
        }
        if (deviceControls.systemBars) row { BookDocumentReaderNavigationBarSettings(bindings.showNavigationBar) }
        row { BookDocumentReaderShowProgressSetting(bindings.showReadingProgress) }
    }
    BookDocumentReaderProgressStyleSettings(
        showProgressBinding = bindings.showReadingProgress,
        styleBinding = bindings.readingProgressStyle,
        available = progressStyleAvailable,
    )
}
