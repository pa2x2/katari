package app.katari.presentation.kit.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

// The window dims behind every dialog whatever its properties say; ClearDialogWindowDim takes it back
internal actual fun kitDialogProperties(
    base: DialogProperties,
    dismissOnClickOutside: Boolean,
    fitsSystemWindows: Boolean?,
    dimsBehind: Boolean,
): DialogProperties = DialogProperties(
    dismissOnBackPress = base.dismissOnBackPress,
    dismissOnClickOutside = dismissOnClickOutside,
    securePolicy = base.securePolicy,
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = fitsSystemWindows ?: base.decorFitsSystemWindows,
)

@Composable
internal actual fun ClearDialogWindowDim() {
    val view = LocalView.current
    SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }
}

@Composable
internal actual fun DialogBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
