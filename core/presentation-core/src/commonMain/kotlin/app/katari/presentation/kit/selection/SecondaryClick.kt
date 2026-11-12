package app.katari.presentation.kit.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced

/**
 * Opens a control's own menu on a right-click, such as a chapter's download button opening its download menu. A
 * [selectableItem] around the control leaves the press to it rather than opening the item's menu.
 */
@Composable
internal fun Modifier.secondaryClick(enabled: Boolean, onClick: () -> Unit): Modifier {
    val items = LocalSelectableItems.current
    val claim = remember { SecondaryClickClaim() }
    if (items != null && enabled) {
        DisposableEffect(items, claim) {
            items.claims += claim
            onDispose { items.claims -= claim }
        }
    }
    val currentOnClick by rememberUpdatedState(onClick)
    if (!enabled) return this
    return onPlaced { claim.coordinates = it }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type != PointerEventType.Press || !event.buttons.isSecondaryPressed) continue
                    val change = event.changes.firstOrNull { it.pressed && !it.previousPressed && !it.isConsumed }
                        ?: continue
                    change.consume()
                    currentOnClick()
                }
            }
        }
}

/** Where a control that takes its own right-clicks sits, so the item around it leaves those to it. */
internal class SecondaryClickClaim {
    var coordinates: LayoutCoordinates? = null

    fun covers(windowPosition: Offset): Boolean =
        coordinates?.takeIf { it.isAttached }?.boundsInWindow()?.contains(windowPosition) == true
}
