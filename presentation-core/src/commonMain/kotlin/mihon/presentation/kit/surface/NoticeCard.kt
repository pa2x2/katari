package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.Warning
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonSize
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** What a [NoticeCard] tells: something worth knowing, or something that may go wrong. */
enum class NoticeTone { Info, Warning }

/**
 * A notice that sits among a screen's or sheet's cards, inset like them, such as an extension no store updates any
 * more or a source paused for library updates. [icon] replaces the tone's own icon. [onClick] makes the whole card the
 * way to resolve it, such as granting a permission; [actionLabel] instead puts one action at its end, such as Resume.
 */
@Composable
fun NoticeCard(
    text: String,
    tone: NoticeTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val spacing = KatariTheme.spacing
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.l,
                    end = if (actionLabel != null) spacing.xs else spacing.l,
                    top = if (actionLabel != null) spacing.xs else spacing.m,
                    bottom = if (actionLabel != null) spacing.xs else spacing.m,
                ),
            horizontalArrangement = Arrangement.spacedBy(spacing.m),
            verticalAlignment = if (actionLabel != null) Alignment.CenterVertically else Alignment.Top,
        ) {
            Icon(icon ?: tone.icon, contentDescription = null)
            Text(text = text, modifier = Modifier.weight(1f), role = TextRole.Body)
            if (actionLabel != null) {
                Button(label = actionLabel, onClick = onAction, style = ButtonStyle.Text, size = ButtonSize.Small)
            }
        }
    }
    val inset = modifier.padding(horizontal = spacing.m)
    val container = when (tone) {
        NoticeTone.Info -> SurfaceContainer.SecondaryContainer
        NoticeTone.Warning -> SurfaceContainer.WarningContainer
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = inset,
            container = container,
            shape = KatariTheme.shapes.large,
            content = content,
        )
    } else {
        Surface(modifier = inset, container = container, shape = KatariTheme.shapes.large, content = content)
    }
}

private val NoticeTone.icon: ImageVector
    get() = when (this) {
        NoticeTone.Info -> MaterialSymbols.Rounded.Info
        NoticeTone.Warning -> MaterialSymbols.Rounded.Warning
    }
