package mihon.presentation.kit.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.DialogProperties
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.LocalRowInset
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A dialog button. A destructive confirm action shows as a filled red button, every other action as text. */
class DialogAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
)

/**
 * The app's dialog: a title, [content], and actions on the right with [neutral] and [dismiss] before [confirm]. Use
 * [ConfirmDialog] when the content is a single message. Rows in [content] drop their side inset, since the dialog
 * already insets its content. [titleAction] sits at the title's end, such as a button that opens the guide.
 */
@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    title: String?,
    confirm: DialogAction?,
    modifier: Modifier = Modifier,
    dismiss: DialogAction? = null,
    neutral: DialogAction? = null,
    icon: ImageVector? = null,
    titleAction: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
    content: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { confirm?.let { DialogButton(it, confirm = true) } },
        modifier = modifier,
        dismissButton = if (neutral != null || dismiss != null) {
            {
                neutral?.let { DialogButton(it, confirm = false) }
                dismiss?.let { DialogButton(it, confirm = false) }
            }
        } else {
            null
        },
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        title = title?.let {
            {
                if (titleAction == null) {
                    Text(it)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(it, modifier = Modifier.weight(1f))
                        titleAction()
                    }
                }
            }
        },
        text = content?.let {
            { CompositionLocalProvider(LocalRowInset provides KatariTheme.sizes.none, content = it) }
        },
        properties = properties,
    )
}

/** Asks to confirm one action. [destructive] marks an action that deletes or discards something. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String?,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    dismissLabel: String = stringResource(MR.strings.action_cancel),
    icon: ImageVector? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = title,
        confirm = DialogAction(confirmLabel, onConfirm, enabled = confirmEnabled, destructive = destructive),
        modifier = modifier,
        dismiss = DialogAction(dismissLabel, onDismissRequest),
        icon = icon,
        content = text?.let { { Text(it) } },
    )
}

@Composable
private fun DialogButton(action: DialogAction, confirm: Boolean) {
    Button(
        label = action.label,
        onClick = action.onClick,
        style = if (confirm && action.destructive) ButtonStyle.Filled else ButtonStyle.Text,
        enabled = action.enabled,
        destructive = action.destructive,
    )
}
