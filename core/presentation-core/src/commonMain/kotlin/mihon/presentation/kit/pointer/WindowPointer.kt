package mihon.presentation.kit.pointer

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the window is worked with a mouse. Only then does the kit show what needs one: right-click menus and Ctrl- and
 * Shift-click selection, scrollbars, tooltips and row buttons on hover, the hand cursor over what a click acts on, and
 * text that can be selected and copied.
 */
internal val LocalWindowHasMouse = staticCompositionLocalOf { PlatformWindowHasMouse }

/**
 * Desktop windows have a mouse. Android windows count as touch whatever input devices are attached, so a mouse there
 * keeps the touch behaviour.
 */
internal expect val PlatformWindowHasMouse: Boolean
