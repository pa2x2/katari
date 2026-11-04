package eu.kanade.presentation.util

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import mihon.presentation.kit.shortcut.KeyboardShortcutScope

/**
 * A screen that stays composed while screens pushed over it show, as an Android activity lives on under the one
 * started over it, rather than being composed anew when it is back on top: a player keeps its stream, position and
 * window state. While covered it reads [LocalScreenCovered] as true, isn't drawn, and takes no pointer, key or back
 * input.
 */
interface StaysComposedUnderCover

/** True while a screen that [StaysComposedUnderCover] is covered by another. */
val LocalScreenCovered = compositionLocalOf { false }

/** [content], a screen that stays composed under cover, laid out but neither placed nor given input while [covered]. */
@Composable
internal fun CoverableScreen(covered: Boolean, content: @Composable () -> Unit) {
    val backHandlers = rememberNavigationEventDispatcherOwner(enabled = !covered)
    CompositionLocalProvider(
        LocalScreenCovered provides covered,
        LocalNavigationEventDispatcherOwner provides backHandlers,
    ) {
        KeyboardShortcutScope(active = !covered) {
            // Left unplaced, it draws nothing and no pointer reaches it, while its effects keep running
            Box(
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { if (!covered) placeable.place(0, 0) }
                },
            ) { content() }
        }
    }
}
