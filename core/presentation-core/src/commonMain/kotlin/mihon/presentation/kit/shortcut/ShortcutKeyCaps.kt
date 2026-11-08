package mihon.presentation.kit.shortcut

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Each way to trigger a shortcut as key caps, such as Ctrl and F for Ctrl+F, the alternatives side by side. */
@Composable
fun ShortcutKeyCaps(keys: List<ShortcutKeys>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        keys.forEach { ShortcutKeysCaps(it) }
    }
}

@Composable
private fun ShortcutKeysCaps(keys: ShortcutKeys) {
    when (keys) {
        is ShortcutKeys.Chord -> Row(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs)) {
            if (keys.ctrl) KeyCap(stringResource(MR.strings.key_ctrl))
            if (keys.alt) KeyCap(stringResource(MR.strings.key_alt))
            if (keys.shift) KeyCap(stringResource(MR.strings.key_shift))
            KeyCap(keys.keyName)
        }
        ShortcutKeys.MouseBack -> Text(stringResource(MR.strings.key_mouse_back), role = TextRole.Supporting)
    }
}

@Composable
private fun KeyCap(name: String) {
    Surface(container = SurfaceContainer.Surface, shape = KatariTheme.shapes.extraSmall, outlined = true) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.s, vertical = KatariTheme.spacing.xxs),
            role = TextRole.Label,
            maxLines = 1,
        )
    }
}
