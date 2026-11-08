package app.katari.presentation.kit.selection

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.OpenInFull
import app.katari.presentation.kit.menu.DropdownMenuDivider
import app.katari.presentation.kit.menu.DropdownMenuItem
import app.katari.presentation.kit.menu.NestedMenuItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * An item's right-click menu: Open and the item's own actions, then the [selectionActions] in the order of the
 * selection bar and its More menu. An item action the selection's actions already offer under the same name, such as a
 * chapter's swipe to mark it read, is listed once, among the selection's.
 */
@Composable
internal fun ColumnScope.ItemMenuItems(
    menu: ItemMenu,
    selectionActions: SelectionActions?,
    onOpen: () -> Unit,
) {
    val selectionGroups = selectionActions
        ?.let { listOf(it.actions) + it.overflow }
        ?.filter { it.isNotEmpty() }
        .orEmpty()
    val selectionLabels = selectionGroups.flatten().mapTo(mutableSetOf()) { it.label }
    val itemActions = menu.item.actions.filter { it.label !in selectionLabels }
    val pick = { action: () -> Unit ->
        menu.picked = true
        action()
    }

    if (menu.item.onOpen != null) {
        DropdownMenuItem(
            label = stringResource(MR.strings.action_open),
            onClick = onOpen,
            icon = MaterialSymbols.Rounded.OpenInFull,
        )
    }
    itemActions.forEach { ItemActionMenuItem(it, pick) }
    selectionGroups.forEachIndexed { index, group ->
        if (index > 0 || menu.item.onOpen != null || itemActions.isNotEmpty()) DropdownMenuDivider()
        group.forEach { ItemActionMenuItem(it, pick) }
    }
}

@Composable
private fun ItemActionMenuItem(action: ItemAction, pick: (() -> Unit) -> Unit) {
    if (action.choices.isEmpty()) {
        DropdownMenuItem(label = action.label, onClick = { pick(action.onClick) }, icon = action.icon)
    } else {
        NestedMenuItem(label = action.label, icon = action.icon) {
            action.choices.forEach { choice ->
                DropdownMenuItem(label = choice.label, onClick = { pick(choice.onClick) }, icon = choice.icon)
            }
        }
    }
}
