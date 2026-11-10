package app.katari.presentation.kit.pointer

import androidx.compose.ui.input.pointer.PointerIcon

// Android windows count as touch (LocalWindowHasMouse), so it is never shown there
internal actual val MovePointer: PointerIcon = PointerIcon.Default
