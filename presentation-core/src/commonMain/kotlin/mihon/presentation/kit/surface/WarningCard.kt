package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Warning
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * A warning that sits among a screen's cards, inset like them, such as an extension no store updates any more.
 * [onClick] makes the whole card the way to resolve it, such as granting a permission.
 */
@Composable
fun WarningCard(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        ) {
            Icon(MaterialSymbols.Rounded.Warning, contentDescription = null)
            Text(text = text, role = TextRole.Body)
        }
    }
    val inset = modifier.padding(horizontal = KatariTheme.spacing.m)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = inset,
            container = SurfaceContainer.WarningContainer,
            shape = KatariTheme.shapes.large,
            content = content,
        )
    } else {
        Surface(
            modifier = inset,
            container = SurfaceContainer.WarningContainer,
            shape = KatariTheme.shapes.large,
            content = content,
        )
    }
}
