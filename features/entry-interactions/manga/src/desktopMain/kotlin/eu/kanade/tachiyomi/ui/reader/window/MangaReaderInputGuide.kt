package eu.kanade.tachiyomi.ui.reader.window

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.katari.entry.interactions.manga.reader.settings.MangaReaderPreferences
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Keyboard
import app.katari.icons.materialsymbols.rounded.Mouse
import app.katari.icons.materialsymbols.rounded.SwipeVertical
import app.katari.presentation.kit.gesture.InputGuide
import app.katari.presentation.kit.gesture.InputGuideHint
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * Whether the guide to the mouse and keys is due over the pages, [shown] or not yet: until it is dismissed once in the
 * profile.
 */
@Composable
internal fun inputGuideDue(preferences: MangaReaderPreferences, shown: Boolean): Boolean {
    val dismissed by preferences.inputGuideShown.collectAsState()
    return shown && !dismissed
}

/**
 * The basics of the mouse and keys over the pages: the clicks, the wheel, and the keys most reached for, with ? for
 * the rest.
 */
@Composable
internal fun MangaReaderInputGuide(preferences: MangaReaderPreferences) {
    InputGuide(
        hints = listOf(
            InputGuideHint(
                icon = MaterialSymbols.Rounded.Mouse,
                title = stringResource(MR.strings.reader_guide_click),
                details = listOf(
                    stringResource(MR.strings.reader_guide_double_click),
                    stringResource(MR.strings.reader_guide_right_click),
                ),
            ),
            InputGuideHint(
                icon = MaterialSymbols.Rounded.SwipeVertical,
                title = stringResource(MR.strings.reader_guide_scroll),
                details = listOf(
                    stringResource(MR.strings.reader_guide_scroll_zoomed),
                    stringResource(MR.strings.reader_guide_scroll_zoom),
                ),
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
