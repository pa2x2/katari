package app.katari.presentation.kit.shortcut

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

/**
 * Runs [onTrigger] on [shortcut]'s keys while this is composed and [enabled], in a window that has keyboard shortcuts.
 * Declare it beside the control that runs the same action, so both stay one action.
 *
 * With [onHoldChange], a key held down until the keyboard repeats it calls it with true, and with false when the key
 * comes up, such as the player's Space playing faster while held; [onTrigger] then runs on a tap, as the key comes up.
 */
@Composable
fun KeyboardShortcutHandler(
    shortcut: KeyboardShortcut,
    enabled: Boolean = true,
    onHoldChange: ((held: Boolean) -> Unit)? = null,
    onTrigger: () -> Unit,
) {
    val registry = LocalKeyboardShortcuts.current ?: return
    val layer = LocalShortcutLayer.current ?: registry.rootLayer
    val active = enabled && LocalShortcutsActive.current
    val registration = remember(registry, layer) {
        ShortcutRegistration(layer, shortcut, active, onTrigger, onHoldChange)
    }
    SideEffect {
        registration.shortcut = shortcut
        registration.enabled = active
        registration.action = onTrigger
        registration.onHoldChange = onHoldChange
    }
    DisposableEffect(registration) {
        registry.register(registration)
        onDispose { registry.unregister(registration) }
    }
}

/**
 * Keeps the shortcuts declared in [content] from running while it isn't [active], for content composed ahead of being
 * shown, such as a pager's neighbouring pages.
 */
@Composable
fun KeyboardShortcutScope(active: Boolean, content: @Composable () -> Unit) {
    if (LocalKeyboardShortcuts.current == null) {
        content()
        return
    }
    CompositionLocalProvider(LocalShortcutsActive provides (active && LocalShortcutsActive.current), content = content)
}

internal val LocalShortcutsActive = compositionLocalOf { true }
