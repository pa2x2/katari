package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import mihon.presentation.kit.theme.KatariTheme

/**
 * A card floating over the screen where [positionProvider] places it, such as a translation beside selected text.
 * It is non-modal: it takes no focus, taps around it reach the content beneath, and back and dismissal are left to
 * the caller. The position provider keeps it on screen, so the window does not clip it. A width in [modifier] is
 * capped so short content does not stretch across a tablet.
 */
@Composable
fun FloatingPanel(
    positionProvider: PopupPositionProvider,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Popup(popupPositionProvider = positionProvider, properties = floatingPanelProperties) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .widthIn(max = FloatingPanelMaxWidth)
                .then(modifier),
            shape = KatariTheme.shapes.extraLarge,
            color = KatariTheme.colors.surfaceContainerHigh,
            contentColor = KatariTheme.colors.onSurface,
            shadowElevation = FloatingPanelShadow,
            content = content,
        )
    }
}

private val FloatingPanelMaxWidth = 360.dp

private val FloatingPanelShadow = 8.dp

private val floatingPanelProperties = PopupProperties(
    focusable = false,
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
    clippingEnabled = false,
)
