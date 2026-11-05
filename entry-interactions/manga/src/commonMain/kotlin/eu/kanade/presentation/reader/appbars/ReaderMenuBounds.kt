package eu.kanade.presentation.reader.appbars

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * Where the reader's menus reach while they are shown, measured by [ReaderAppBars] so overlays drawn in the same
 * window, such as translate mode's toolbar, can keep clear of them. Positions are pixels from the top of the root;
 * they are null until the menu has been laid out once and keep the last measured value while it is hidden.
 */
@Stable
internal class ReaderMenuBounds {
    var topMenuBottom by mutableStateOf<Float?>(null)
        private set

    var bottomMenuTop by mutableStateOf<Float?>(null)
        private set

    fun Modifier.measuresTopMenu(): Modifier = onGloballyPositioned { coordinates ->
        topMenuBottom = coordinates.positionInRoot().y + coordinates.size.height
    }

    fun Modifier.measuresBottomMenu(): Modifier = onGloballyPositioned { coordinates ->
        bottomMenuTop = coordinates.positionInRoot().y
    }
}
