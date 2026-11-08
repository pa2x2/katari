package mihon.entry.interactions.book.reader.window

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderPreferences
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Keyboard
import mihon.icons.materialsymbols.rounded.Mouse
import mihon.icons.materialsymbols.rounded.SwipeVertical
import mihon.presentation.kit.gesture.InputGuide
import mihon.presentation.kit.gesture.InputGuideHint
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * Whether the guide to the mouse and keys is due over the book, [shown] or not yet: until it is dismissed once in the
 * profile.
 */
@Composable
internal fun bookInputGuideDue(preferences: BookDocumentReaderPreferences, shown: Boolean): Boolean {
    val dismissed by preferences.inputGuideShown.collectAsState()
    return shown && !dismissed
}

/**
 * The basics of the mouse and keys over the book: the clicks, dragging to select and the selection's menu, the wheel
 * and the text size, and the keys most reached for, with ? for the rest.
 */
@Composable
internal fun BookReaderInputGuide(preferences: BookDocumentReaderPreferences) {
    InputGuide(
        hints = listOf(
            InputGuideHint(
                icon = MaterialSymbols.Rounded.Mouse,
                title = stringResource(MR.strings.reader_guide_click),
                details = listOf(
                    stringResource(MR.strings.book_reader_guide_drag),
                    stringResource(MR.strings.book_reader_guide_right_click),
                ),
            ),
            InputGuideHint(
                icon = MaterialSymbols.Rounded.SwipeVertical,
                title = stringResource(MR.strings.reader_guide_scroll),
                details = listOf(stringResource(MR.strings.book_reader_guide_scroll_text_size)),
            ),
            InputGuideHint(
                icon = MaterialSymbols.Rounded.Keyboard,
                title = stringResource(MR.strings.reader_guide_keys_turn),
                details = listOf(
                    stringResource(MR.strings.reader_guide_keys_chapter),
                    stringResource(MR.strings.reader_guide_keys_list),
                ),
            ),
        ),
        onDismiss = { preferences.inputGuideShown.set(true) },
        modifier = Modifier.fillMaxSize(),
    )
}
