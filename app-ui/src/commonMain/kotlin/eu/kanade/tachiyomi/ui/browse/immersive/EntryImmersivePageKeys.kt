package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.shortcut.KeyboardShortcutHandler
import app.katari.presentation.kit.shortcut.ShortcutGroup
import app.katari.presentation.kit.shortcut.ShortcutKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * ↑ and ↓ move to the previous and next page, where the window has keyboard shortcuts, as the wheel does. They take
 * the keys before a focused control, so they never move focus instead, unless the page holds them ([enabled] false).
 * A key pressed while the pager still moves goes on from the page it is moving to.
 */
@Composable
internal fun EntryImmersivePageKeys(pagerState: PagerState, enabled: Boolean) {
    val scope = rememberCoroutineScope()
    KeyboardShortcutHandler(
        shortcut = previousTitleShortcut(),
        enabled = enabled && pagerState.targetPage > 0,
    ) { pagerState.stepTitle(scope, -1) }
    KeyboardShortcutHandler(
        shortcut = nextTitleShortcut(),
        enabled = enabled && pagerState.targetPage < pagerState.pageCount - 1,
    ) { pagerState.stepTitle(scope, 1) }
}

/** Moves [by] titles from the one the pager is showing or moving to. */
internal fun PagerState.stepTitle(scope: CoroutineScope, by: Int) {
    val page = (targetPage + by).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    scope.launch { animateScrollToPage(page) }
}

@Composable
internal fun previousTitleShortcut() = pageShortcut(
    id = "immersive.previous",
    label = stringResource(MR.strings.browse_immersive_previous),
    key = ShortcutKeys.Chord(Key.DirectionUp, "↑"),
)

@Composable
internal fun nextTitleShortcut() = pageShortcut(
    id = "immersive.next",
    label = stringResource(MR.strings.browse_immersive_next),
    key = ShortcutKeys.Chord(Key.DirectionDown, "↓"),
)

private fun pageShortcut(id: String, label: String, key: ShortcutKeys) = KeyboardShortcut(
    id = id,
    label = label,
    keys = listOf(key),
    group = ShortcutGroup.CurrentScreen,
    whileTyping = false,
    beforeFocus = true,
)
