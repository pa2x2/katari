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
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    // The theme's amount, read before this changes it, so a sheet whose edge changes with the window can go back to it
    val themeAmount = remember(window) { window.attributes.dimAmount }
    SideEffect {
        window.setDimAmount(
            when (dim) {
                DialogDim.Full -> themeAmount
                DialogDim.Light -> themeAmount * LIGHT_DIM_FRACTION
                DialogDim.None -> 0f
            },
        )
    }
}

@Composable
internal actual fun DialogBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
