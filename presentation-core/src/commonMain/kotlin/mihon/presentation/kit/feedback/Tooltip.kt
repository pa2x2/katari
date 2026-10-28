package mihon.presentation.kit.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
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

/** True inside a [Tooltip]'s anchor, so an icon button there doesn't add a tooltip of its own. */
internal val LocalInTooltip = staticCompositionLocalOf { false }
