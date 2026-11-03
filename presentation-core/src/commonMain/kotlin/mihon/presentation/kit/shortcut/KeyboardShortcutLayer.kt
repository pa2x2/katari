package mihon.presentation.kit.shortcut

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/** An overlay's layer of shortcuts, which covers the shortcuts of the screen under it while it is shown. */
@Composable
internal fun KeyboardShortcutLayer(content: @Composable () -> Unit) {
    val registry = LocalKeyboardShortcuts.current
    if (registry == null) {
        content()
        return
    }
    val layer = remember(registry) { ShortcutLayer() }
    DisposableEffect(layer) {
        registry.open(layer)
        onDispose { registry.close(layer) }
    }
    CompositionLocalProvider(LocalShortcutLayer provides layer, content = content)
}

/** Null at the window's own level, under every overlay. */
internal val LocalShortcutLayer = staticCompositionLocalOf<ShortcutLayer?> { null }
