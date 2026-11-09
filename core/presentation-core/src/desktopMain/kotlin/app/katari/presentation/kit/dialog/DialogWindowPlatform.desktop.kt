package app.katari.presentation.kit.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

internal actual fun kitDialogProperties(
    base: DialogProperties,
    dismissOnClickOutside: Boolean,
    fitsSystemWindows: Boolean?,
    dim: DialogDim,
): DialogProperties = DialogProperties(
    dismissOnBackPress = base.dismissOnBackPress,
    dismissOnClickOutside = dismissOnClickOutside,
    usePlatformDefaultWidth = false,
    usePlatformInsets = fitsSystemWindows ?: base.usePlatformInsets,
    scrimColor = when (dim) {
        DialogDim.Full -> base.scrimColor
        DialogDim.Light -> base.scrimColor.copy(alpha = base.scrimColor.alpha * LIGHT_DIM_FRACTION)
        DialogDim.None -> Color.Transparent
    },
)

// The scrim is already set by kitDialogProperties
@Composable
internal actual fun DialogWindowDim(dim: DialogDim) = Unit

@Composable
internal actual fun DialogBackHandler(enabled: Boolean, onBack: () -> Unit) {
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = enabled,
        onBackCompleted = onBack,
    )
}
