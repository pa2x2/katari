package app.katari.presentation.kit.pointer

import androidx.compose.ui.input.pointer.PointerIcon

/**
 * No pointer at all, for a media viewer to show while the mouse rests over the picture or page. Android windows count
 * as touch ([LocalWindowHasMouse]), so there it is the default pointer.
 */
expect val HiddenPointer: PointerIcon
