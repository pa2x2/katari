package mihon.presentation.kit.pointer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

/** The hand cursor over a row, tile or card a click acts on, in a window worked with a mouse. */
@Composable
internal fun Modifier.clickCursor(enabled: Boolean = true): Modifier =
    if (enabled && LocalWindowHasMouse.current) pointerHoverIcon(PointerIcon.Hand) else this
