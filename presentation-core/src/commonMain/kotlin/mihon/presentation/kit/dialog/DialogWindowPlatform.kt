package mihon.presentation.kit.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

/**
 * Window properties for the kit's dialogs and sheets, which size their own content, so the platform's default width is
 * off. [base] carries the caller's back and secure-screen choices. [fitsSystemWindows] keeps the window out of the
 * system bars; null keeps [base]'s choice. Without [dimsBehind] nothing shades the screen behind the window.
 */
internal expect fun kitDialogProperties(
    base: DialogProperties = DialogProperties(),
    dismissOnClickOutside: Boolean = base.dismissOnClickOutside,
    fitsSystemWindows: Boolean? = null,
    dimsBehind: Boolean = true,
): DialogProperties

/** Undoes the dim a platform draws behind every dialog window regardless of its properties; call inside the dialog. */
@Composable
internal expect fun ClearDialogWindowDim()

@Composable
internal expect fun DialogBackHandler(enabled: Boolean, onBack: () -> Unit)
