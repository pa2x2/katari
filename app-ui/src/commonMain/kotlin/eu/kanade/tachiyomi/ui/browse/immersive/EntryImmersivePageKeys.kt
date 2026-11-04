package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.launch
import mihon.presentation.kit.shortcut.KeyboardShortcut
import mihon.presentation.kit.shortcut.KeyboardShortcutHandler
import mihon.presentation.kit.shortcut.ShortcutGroup
import mihon.presentation.kit.shortcut.ShortcutKeys
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
    val step: (Int) -> Unit = { by ->
        val page = (pagerState.targetPage + by).coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
        scope.launch { pagerState.animateScrollToPage(page) }
    }
    KeyboardShortcutHandler(
        shortcut = pageShortcut(
            id = "immersive.previous",
            label = stringResource(MR.strings.browse_immersive_previous),
            key = ShortcutKeys.Chord(Key.DirectionUp, "↑"),
        ),
        enabled = enabled && pagerState.targetPage > 0,
    ) { step(-1) }
    KeyboardShortcutHandler(
        shortcut = pageShortcut(
            id = "immersive.next",
            label = stringResource(MR.strings.browse_immersive_next),
            key = ShortcutKeys.Chord(Key.DirectionDown, "↓"),
        ),
        enabled = enabled && pagerState.targetPage < pagerState.pageCount - 1,
    ) { step(1) }
}

private fun pageShortcut(id: String, label: String, key: ShortcutKeys) = KeyboardShortcut(
    id = id,
    label = label,
    keys = listOf(key),
    group = ShortcutGroup.CurrentScreen,
    whileTyping = false,
    beforeFocus = true,
)
