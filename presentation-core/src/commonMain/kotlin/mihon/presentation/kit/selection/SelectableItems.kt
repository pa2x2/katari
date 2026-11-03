package mihon.presentation.kit.selection

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import mihon.presentation.kit.menu.ContextMenu
import mihon.presentation.kit.pointer.LocalWindowHasMouse
import mihon.presentation.kit.shortcut.KeyboardShortcutHandler
import mihon.presentation.kit.shortcut.ScreenShortcuts

/**
 * The items a screen lists, such as library covers or chapter rows, made to answer a mouse. Right-clicking an item
 * ([selectableItem]) opens its menu: Open, the item's own actions, and, where the screen has a [selection], the
 * selection bar's [actions]. With a [selection], Ctrl+click toggles an item, Shift+click selects the range to it, Ctrl+A
 * selects every item and Esc clears the selection before it goes back. The screen keeps owning its selection; this
 * only routes the mouse and the keys to it.
 *
 * Right-clicking an item outside the selection selects just that item, as file managers do, so the selection's actions
 * act on it. Dismissing the menu without picking anything, or picking Open, clears that selection again.
 *
 * [content] is one layout, such as a list or a pager of grids. A touch window gets [content] as it is; the keys only run
 * in a window with keyboard shortcuts.
 */
@Composable
fun SelectableItems(
    selection: ItemSelection?,
    actions: SelectionActions? = null,
    content: @Composable () -> Unit,
) {
    if (selection != null) {
        KeyboardShortcutHandler(ScreenShortcuts.SelectAll, onTrigger = selection.onSelectAll)
        KeyboardShortcutHandler(
            ScreenShortcuts.ClearSelection,
            enabled = selection.active,
            onTrigger = selection.onClear,
        )
    }
    if (!LocalWindowHasMouse.current) {
        content()
        return
    }

    val state = remember { SelectableItemsState() }
    SideEffect { state.selection = selection }
    Box(modifier = Modifier.onPlaced { state.coordinates = it }, propagateMinConstraints = true) {
        CompositionLocalProvider(LocalSelectableItems provides state, content = content)
        val menu = state.menu
        if (menu != null) {
            ContextMenu(position = menu.position, onDismissRequest = { state.closeMenu(menu) }) {
                ItemMenuItems(
                    menu = menu,
                    selectionActions = actions.takeIf { selection != null },
                    onOpen = { state.open(menu) },
                )
            }
        }
    }
}

/** A screen's selection, as [SelectableItems] drives it. [active] while anything is selected. */
@Immutable
class ItemSelection(
    val active: Boolean,
    val onSelectAll: () -> Unit,
    val onClear: () -> Unit,
)

/** What an item does under the mouse, as [selectableItem] was last composed with it. */
internal class ItemPointer(
    val selected: Boolean,
    val onToggle: (() -> Unit)?,
    val onSelectRange: (() -> Unit)?,
    val onOpen: (() -> Unit)?,
    val actions: List<ItemAction>,
)

/**
 * An open item menu at [position] in the items' coordinates. [selectedItem] when opening it selected the item, so the
 * selection is the menu's own to clear again; [picked] once an action has been chosen, before the menu closes.
 */
internal class ItemMenu(
    val position: Offset,
    val item: ItemPointer,
    val itemCoordinates: LayoutCoordinates,
    val selectedItem: Boolean,
) {
    var picked = false
}

@Stable
internal class SelectableItemsState {
    var selection: ItemSelection? = null
    var coordinates: LayoutCoordinates? = null
    var menu by mutableStateOf<ItemMenu?>(null)
        private set

    fun openMenu(item: ItemPointer, itemCoordinates: LayoutCoordinates, position: Offset) {
        val coordinates = coordinates?.takeIf { it.isAttached } ?: return
        if (!itemCoordinates.isAttached) return
        val inItems = coordinates.localPositionOf(itemCoordinates, position)
        // A right-click can reach an item before the open menu learns it was dismissed
        val previous = menu
        if (previous != null && previous.itemCoordinates === itemCoordinates) {
            menu = ItemMenu(inItems, item, itemCoordinates, previous.selectedItem)
            return
        }
        previous?.let(::closeMenu)
        val selection = selection
        val selectsItem = selection != null && item.onToggle != null && !item.selected
        if (selectsItem) {
            selection.onClear()
            item.onToggle()
        }
        menu = ItemMenu(inItems, item, itemCoordinates, selectsItem)
    }

    fun open(menu: ItemMenu) {
        menu.picked = true
        if (menu.selectedItem) selection?.onClear()
        menu.item.onOpen?.invoke()
    }

    fun closeMenu(menu: ItemMenu) {
        if (this.menu !== menu) return
        if (menu.selectedItem && !menu.picked) selection?.onClear()
        this.menu = null
    }
}

internal val LocalSelectableItems = staticCompositionLocalOf<SelectableItemsState?> { null }
