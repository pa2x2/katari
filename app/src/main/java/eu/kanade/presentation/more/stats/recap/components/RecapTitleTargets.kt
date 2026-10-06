package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * The on-screen bounds of every title a page shows. The story handles holding the page itself, for pausing, so a
 * hold is matched against these bounds instead of each title listening for it and competing with the story.
 */
internal class RecapTitleTargets {
    private val bounds = mutableMapOf<Any, Pair<Long, Rect>>()

    fun entryAt(rootPosition: Offset): Long? =
        bounds.values.firstOrNull { (_, rect) -> rect.contains(rootPosition) }?.first

    fun set(key: Any, entryId: Long, rect: Rect) {
        bounds[key] = entryId to rect
    }

    fun remove(key: Any) {
        bounds.remove(key)
    }
}

/** Marks this element as showing [entryId], so holding it offers to hide the title. */
@Composable
internal fun Modifier.recapTitleTarget(entryId: Long): Modifier {
    val targets = LocalRecapTitleTargets.current ?: return this
    val key = remember { Any() }
    DisposableEffect(targets, key) { onDispose { targets.remove(key) } }
    return onGloballyPositioned { targets.set(key, entryId, it.boundsInRoot()) }
}
