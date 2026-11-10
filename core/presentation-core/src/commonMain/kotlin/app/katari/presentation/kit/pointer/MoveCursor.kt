package app.katari.presentation.kit.pointer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

/**
 * The move cursor over something a drag moves, such as subtitles in the subtitle editor, in a window worked with a
 * mouse. [enabled] follows whether the pointer is over it, where it covers only part of this element.
 */
@Composable
fun Modifier.moveCursor(enabled: Boolean = true): Modifier =
    if (enabled && LocalWindowHasMouse.current) pointerHoverIcon(MovePointer) else this

internal expect val MovePointer: PointerIcon
