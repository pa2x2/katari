package eu.kanade.presentation.browse.immersive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Shared item position for a regular browse surface and its immersive presentation.
 *
 * Every immersive host should pass the same instance to both surfaces so position changes remain
 * bidirectional without host-specific synchronization callbacks.
 */
@Stable
class EntryImmersivePositionState internal constructor(initialItemIndex: Int) {
    var itemIndex by mutableIntStateOf(initialItemIndex.coerceAtLeast(0))
        private set

    fun updateItemIndex(index: Int) {
        itemIndex = index.coerceAtLeast(0)
    }

    companion object {
        val Saver = Saver<EntryImmersivePositionState, Int>(
            save = { it.itemIndex },
            restore = ::EntryImmersivePositionState,
        )
    }
}

@Composable
internal fun rememberEntryImmersivePositionState(
    resetKey: Any?,
    initialItemIndex: Int = 0,
): EntryImmersivePositionState {
    return rememberSaveable(resetKey, saver = EntryImmersivePositionState.Saver) {
        EntryImmersivePositionState(initialItemIndex)
    }
}
