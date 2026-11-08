package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
 * [dismissLabel] puts a way to put the notice off before that action, such as Not now on the Library's suggestion to
 * install missing extensions.
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
    dismissLabel: String? = null,
    onDismiss: () -> Unit = {},
) {
    val spacing = KatariTheme.spacing
    val content: @Composable () -> Unit = {
        if (dismissLabel != null) {
            // Two actions beside the text would squeeze it into a narrow column, so they go under it
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.l, end = spacing.xs, top = spacing.m, bottom = spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(spacing.m),
            ) {
                Icon(icon ?: tone.icon, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = text, modifier = Modifier.padding(end = spacing.m), role = TextRole.Body)
                    Row(modifier = Modifier.align(Alignment.End)) {
                        Button(
                            label = dismissLabel,
                            onClick = onDismiss,
                            style = ButtonStyle.Text,
                            size = ButtonSize.Small,
                        )
                        if (actionLabel != null) {
                            Button(
                                label = actionLabel,
                                onClick = onAction,
                                style = ButtonStyle.Text,
                                size = ButtonSize.Small,
                            )
                        }
                    }
                }
            }
        } else {
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
