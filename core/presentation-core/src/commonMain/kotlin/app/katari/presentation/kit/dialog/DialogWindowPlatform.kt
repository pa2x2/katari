package app.katari.presentation.kit.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

/**
 * Window properties for the kit's dialogs and sheets, which size their own content, so the platform's default width is
 * off. [base] carries the caller's back and secure-screen choices. [fitsSystemWindows] keeps the window out of the
 * system bars; null keeps [base]'s choice. [dim] says how much the screen behind the window is shaded; with [DialogDim.None]
 * nothing shades the screen behind the window.
 */
internal expect fun kitDialogProperties(
    base: DialogProperties = DialogProperties(),
    dismissOnClickOutside: Boolean = base.dismissOnClickOutside,
    fitsSystemWindows: Boolean? = null,
    dim: DialogDim = DialogDim.Full,
): DialogProperties

/**
 * Sets the dim a platform draws behind every dialog window regardless of its properties to [dim]; call inside the
 * dialog.
 */
@Composable
internal expect fun DialogWindowDim(dim: DialogDim)

/** How much a dialog or sheet window shades the screen behind it. */
internal enum class DialogDim {
    /** The platform's dim, setting the window apart from what it covers. */
    Full,

    /** A part of the platform's dim, so what is behind stays readable, such as a list a sheet filters as it changes. */
    Light,

    /** None. */
    None,
}

// The part of the platform's dim a light dim keeps
internal const val LIGHT_DIM_FRACTION = 0.4f

@Composable
internal expect fun DialogBackHandler(enabled: Boolean, onBack: () -> Unit)
