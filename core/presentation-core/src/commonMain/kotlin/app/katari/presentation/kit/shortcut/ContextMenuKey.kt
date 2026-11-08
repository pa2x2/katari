package app.katari.presentation.kit.shortcut

import androidx.compose.ui.input.key.Key

/**
 * The key that opens a context menu, often beside the right Ctrl. On the desktop Compose's [Key.Menu] is a placeholder
 * that no key sends, so a shortcut on this key uses this instead.
 */
expect val ContextMenuKey: Key
