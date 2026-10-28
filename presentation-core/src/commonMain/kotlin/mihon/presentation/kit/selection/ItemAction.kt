package mihon.presentation.kit.selection

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Something done to an item or a selection, as a selection bar shows it and an item's right-click menu lists it.
 * [choices] make it open a menu of them instead of acting itself, such as how many chapters to download.
 */
@Immutable
class ItemAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit = {},
    val choices: List<ItemAction> = emptyList(),
)

/**
 * The actions on a selection: [actions] stand on its selection bar and [overflow] collects in the bar's More menu, with
 * a break between its groups. A selected item's right-click menu lists the same actions, so a screen describes them
 * once.
 */
@Immutable
class SelectionActions(
    val actions: List<ItemAction>,
    val overflow: List<List<ItemAction>> = emptyList(),
)
