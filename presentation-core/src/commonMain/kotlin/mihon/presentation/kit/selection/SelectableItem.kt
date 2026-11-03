package mihon.presentation.kit.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced

/**
 * Makes this item of the enclosing [SelectableItems] answer the mouse. Ctrl+click runs [onToggle], which adds the item
 * to the screen's selection or takes it out, and Shift+click [onSelectRange], which selects the range from the item
 * selected last to this one as a long press does, or [onToggle] where the item has no range. An item that can't be
 * selected leaves both null. [onOpen] is what the item's click does outside a selection, offered as the menu's Open,
 * and [actions] are the item's own, such as removing a history entry.
 *
 * Pass it in the modifier of the item's row, tile or card: it takes the mouse presses it acts on before the item's own
 * click sees them.
 */
@Composable
fun Modifier.selectableItem(
    selected: Boolean,
    onToggle: (() -> Unit)?,
    onSelectRange: (() -> Unit)?,
    onOpen: (() -> Unit)?,
    actions: List<ItemAction> = emptyList(),
): Modifier {
    val items = LocalSelectableItems.current ?: return this
    val pointer by rememberUpdatedState(ItemPointer(selected, onToggle, onSelectRange, onOpen, actions))
    val placement = remember { ItemPlacement() }
    return onPlaced { placement.coordinates = it }
        .pointerInput(items) {
            awaitPointerEventScope {
                while (true) {
                    // Seen before the item's click, which must not also act on these presses
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type != PointerEventType.Press) continue
                    val change = event.changes.firstOrNull { it.pressed && !it.previousPressed } ?: continue
                    val item = pointer
                    val modifiers = event.keyboardModifiers
                    val action: (() -> Unit)? = when {
                        event.buttons.isSecondaryPressed -> {
                            val coordinates = placement.coordinates
                            if (coordinates != null) {
                                { items.openMenu(item, coordinates, change.position) }
                            } else {
                                null
                            }
                        }
                        !event.buttons.isPrimaryPressed -> null
                        modifiers.isCtrlPressed -> item.onToggle
                        modifiers.isShiftPressed -> item.onSelectRange ?: item.onToggle
                        else -> null
                    }
                    if (action != null) {
                        change.consume()
                        action()
                    }
                }
            }
        }
}

private class ItemPlacement {
    var coordinates: LayoutCoordinates? = null
}
