package app.katari.presentation.kit.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

// The window dims behind every dialog whatever its properties say; DialogWindowDim sets how much
internal actual fun kitDialogProperties(
    base: DialogProperties,
    dismissOnClickOutside: Boolean,
    fitsSystemWindows: Boolean?,
    dim: DialogDim,
): DialogProperties = DialogProperties(
    dismissOnBackPress = base.dismissOnBackPress,
    dismissOnClickOutside = dismissOnClickOutside,
    securePolicy = base.securePolicy,
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = fitsSystemWindows ?: base.decorFitsSystemWindows,
)

@Composable
internal actual fun DialogWindowDim(dim: DialogDim) {
    val view = LocalView.current
    val themeAmount = remember { ThemeDimAmount() }
    // The view joins the dialog's window only once composed, so the window is looked up after composition
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        // The theme's amount, read before this changes it, so a sheet whose edge changes with the window can go back
        val base = themeAmount.value ?: window.attributes.dimAmount.also { themeAmount.value = it }
        window.setDimAmount(
            when (dim) {
                DialogDim.Full -> base
                DialogDim.Light -> base * LIGHT_DIM_FRACTION
                DialogDim.None -> 0f
            },
        )
    }
}

/** The dim amount a dialog window had from its theme, before [DialogWindowDim] first changed it. */
private class ThemeDimAmount {
    var value: Float? = null
}

@Composable
internal actual fun DialogBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
