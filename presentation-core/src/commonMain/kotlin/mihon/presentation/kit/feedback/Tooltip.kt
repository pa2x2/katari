package mihon.presentation.kit.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.pointer.LocalWindowHasMouse
import mihon.presentation.kit.shortcut.KeyboardShortcut
import mihon.presentation.kit.shortcut.LocalKeyboardShortcuts
import mihon.presentation.kit.shortcut.ShortcutKeyCaps
import mihon.presentation.kit.theme.KatariTheme

/**
 * Shows [text] above [content] on long press or hover; it never takes focus, so it cannot trap keyboard navigation. In
 * a window with keyboard shortcuts it also shows the keys of [shortcut], the action's shortcut.
 */
@Composable
fun Tooltip(
    text: String,
    modifier: Modifier = Modifier,
    shortcut: KeyboardShortcut? = null,
    content: @Composable () -> Unit,
) {
    val keys = shortcut?.keys?.take(1)?.takeIf { LocalKeyboardShortcuts.current != null }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip {
                if (keys.isNullOrEmpty()) {
                    Text(text)
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text)
                        ShortcutKeyCaps(keys)
                    }
                }
            }
        },
        state = rememberTooltipState(),
        modifier = modifier,
        focusable = false,
    ) {
        CompositionLocalProvider(LocalInTooltip provides true, content = content)
    }
}

/**
 * In a window worked with a mouse, shows [text] above [content] on hover, with the keys of [shortcut] as [Tooltip]
 * does; elsewhere, and inside another tooltip's anchor or for null [text], it shows [content] alone.
 */
@Composable
fun HoverTooltip(text: String?, shortcut: KeyboardShortcut? = null, content: @Composable () -> Unit) {
    if (text != null && LocalWindowHasMouse.current && !LocalInTooltip.current) {
        Tooltip(text, shortcut = shortcut, content = content)
    } else {
        content()
    }
}

/**
 * Shows [text] under [position], where the pointer is in the box this is placed in, for content that names its own
 * tooltips as the pointer moves over it, such as a web page; null shows none. It goes over the box's content, after it.
 */
@Composable
fun BoxScope.PointerTooltip(text: String?, position: Offset) {
    if (text == null) return
    key(text) {
        val state = rememberTooltipState(isPersistent = true)
        LaunchedEffect(state) { state.show() }
        Box(modifier = Modifier.matchParentSize()) {
            // Around the tooltip box, which places the tooltip by its own bounds rather than by its anchor's
            Box(modifier = Modifier.offset { position.round() }) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                    tooltip = { PlainTooltip { Text(text) } },
                    state = state,
                    focusable = false,
                    enableUserInput = false,
                ) {
                    Spacer(modifier = Modifier.size(PointerHeight))
                }
            }
        }
    }
}

// The tooltip goes under the pointer's arrow rather than over it
private val PointerHeight = 20.dp

/** True inside a [Tooltip]'s anchor, so an icon button there doesn't add a tooltip of its own. */
internal val LocalInTooltip = staticCompositionLocalOf { false }
