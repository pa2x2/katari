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
    dimsBehind: Boolean,
): DialogProperties = DialogProperties(
    dismissOnBackPress = base.dismissOnBackPress,
    dismissOnClickOutside = dismissOnClickOutside,
    usePlatformDefaultWidth = false,
    usePlatformInsets = fitsSystemWindows ?: base.usePlatformInsets,
    scrimColor = if (dimsBehind) base.scrimColor else Color.Transparent,
)

// The scrim is already left out by kitDialogProperties
@Composable
internal actual fun ClearDialogWindowDim() = Unit

@Composable
internal actual fun DialogBackHandler(enabled: Boolean, onBack: () -> Unit) {
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = enabled,
        onBackCompleted = onBack,
    )
}
