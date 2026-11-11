package app.katari.entry.interactions.book.reader.window

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.katari.entry.interactions.book.document.reader.settings.BookDocumentReaderPreferences
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Keyboard
import app.katari.icons.materialsymbols.rounded.SwipeVertical
import app.katari.platform.presentation.platformPresentation
import app.katari.presentation.kit.gesture.InputGuide
import app.katari.presentation.kit.gesture.InputGuideHint
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
                icon = platformPresentation.pointerIcon,
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
