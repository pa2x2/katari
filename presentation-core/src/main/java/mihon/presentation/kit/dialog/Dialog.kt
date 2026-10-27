package mihon.presentation.kit.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.ProvideTextRole
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.LocalInDialog
import mihon.presentation.kit.list.LocalRowGroupFramed
import mihon.presentation.kit.list.LocalRowInset
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.window.currentWindowLayout
import mihon.presentation.kit.window.rememberFoldHalf
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A dialog or sheet button. [destructive] marks an action that deletes or discards something. */
class DialogAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
)

/**
 * The app's dialog: an optional [icon], a title, [content], and its actions (see [DialogActions]). Every dialog is
 * the window's width less a margin on each side, up to Material's maximum, whatever its content; while a vertical
 * fold is half-open, it is centred in one half and sized to it instead (see [rememberFoldHalf]). Use [ConfirmDialog]
 * when the content is a single message. Rows in [content] drop their side inset and row groups their card, since the
 * dialog already insets and frames its content; choice rows fill while selected instead. [titleAction] sits at the
 * title's end, such as a button that opens the guide. With an [icon] the title is centred under it.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val half = rememberFoldHalf()
    val panel: @Composable () -> Unit = {
        DialogPanel(
            title = title,
            confirm = confirm,
            dismiss = dismiss,
            neutral = neutral,
            icon = icon,
            titleAction = titleAction,
            content = content,
        )
    }
    if (half == null) {
        val windowWidth = currentWindowLayout().size.width
        BasicAlertDialog(
            onDismissRequest = onDismissRequest,
            // The width is set here rather than as padding inside a full-width window, so a tap in the margin is
            // outside the dialog and dismisses it.
            modifier = modifier.width(dialogWidth(windowWidth)),
            properties = DialogProperties(
                dismissOnBackPress = properties.dismissOnBackPress,
                dismissOnClickOutside = properties.dismissOnClickOutside,
                securePolicy = properties.securePolicy,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = properties.decorFitsSystemWindows,
            ),
            content = panel,
        )
    } else {
        // The platform centres a dialog's content in the window, so a dialog kept to one half of the fold fills the
        // window itself, places the panel and takes over taps outside it.
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = properties.dismissOnBackPress,
                dismissOnClickOutside = false,
                securePolicy = properties.securePolicy,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        enabled = properties.dismissOnClickOutside,
                        onClick = onDismissRequest,
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .absoluteOffset(x = half.left)
                        .width(half.width)
                        .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = modifier
                            .width(dialogWidth(half.width))
                            .clickable(interactionSource = null, indication = null, onClick = {}),
                    ) {
                        panel()
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogPanel(
    title: String?,
    confirm: DialogAction?,
    dismiss: DialogAction?,
    neutral: DialogAction?,
    icon: ImageVector?,
    titleAction: (@Composable () -> Unit)?,
    content: (@Composable () -> Unit)?,
) {
    val spacing = KatariTheme.spacing
    Surface(container = SurfaceContainer.High, shape = KatariTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(spacing.xl)) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = spacing.l),
                    tone = if (confirm?.destructive == true) ContentTone.Error else ContentTone.Secondary,
                )
            }
            if (title != null) {
                DialogTitle(title, centred = icon != null, action = titleAction)
            }
            if (content != null) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(top = if (title != null) spacing.l else KatariTheme.sizes.none),
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides KatariTheme.colors.onSurfaceVariant,
                        LocalRowInset provides KatariTheme.sizes.none,
                        LocalRowGroupFramed provides false,
                        LocalInDialog provides true,
                        LocalDialogConfirm provides confirm,
                    ) {
                        ProvideTextRole(TextRole.Body, content)
                    }
                }
            }
            if (confirm != null || dismiss != null || neutral != null) {
                DialogActions(
                    confirm = confirm,
                    dismiss = dismiss,
                    neutral = neutral,
                    modifier = Modifier.padding(top = spacing.xl),
                )
            }
        }
    }
}

/** The space less a margin on each side, up to Material's maximum. */
private fun dialogWidth(available: Dp): Dp = minOf(available - DialogMargin * 2, DialogMaxWidth)

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
private fun DialogTitle(title: String, centred: Boolean, action: (@Composable () -> Unit)?) {
    if (action == null) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            role = TextRole.SheetTitle,
            textAlign = if (centred) TextAlign.Center else null,
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), role = TextRole.SheetTitle)
            action()
        }
    }
}

/** The open dialog's confirm action, which a single-line text field in it runs from the keyboard's Done key. */
internal val LocalDialogConfirm = compositionLocalOf<DialogAction?> { null }

private val DialogMargin = 24.dp

// Material's maximum dialog width.
private val DialogMaxWidth = 560.dp
