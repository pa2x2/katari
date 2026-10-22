package mihon.presentation.kit.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.feedback.Tooltip
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Icon actions first, each with a tooltip; overflow actions collect in one "More" menu at the end. */
@Composable
fun AppBarActions(actions: List<AppBar.AppBarAction>) {
    // A menu anchors to its parent layout, so each one shares a Box with its button rather than anchoring to the
    // whole actions row.
    actions.filterIsInstance<AppBar.Action>().forEach { action ->
        Box {
            Tooltip(action.title) {
                IconButton(
                    icon = action.icon,
                    contentDescription = action.title,
                    onClick = action.onClick,
                    tone = action.tone,
                    enabled = action.enabled,
                    badgeCount = action.badgeCount,
                )
            }
            action.menu?.invoke()
        }
    }

    val overflowActions = actions.filterIsInstance<AppBar.OverflowAction>()
    if (overflowActions.isNotEmpty()) {
        var showMenu by remember { mutableStateOf(false) }
        Box {
            val description = stringResource(MR.strings.action_menu_overflow_description)
            Tooltip(description) {
                IconButton(
                    icon = MaterialSymbols.Rounded.MoreVert,
                    contentDescription = description,
                    onClick = { showMenu = !showMenu },
                    modifier = Modifier.testTag(OVERFLOW_TEST_TAG),
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                overflowActions.forEach {
                    DropdownMenuItem(label = it.title, onClick = it.onClick, icon = it.icon)
                }
            }
        }
    }
}

/**
 * UI automation, such as the showcase capture scenes, opens the overflow by this to reach an action that may sit either
 * among the icons or in the menu.
 */
private const val OVERFLOW_TEST_TAG = "app_bar_overflow"

sealed interface AppBar {
    sealed interface AppBarAction

    data class Action(
        val title: String,
        val icon: ImageVector,
        val onClick: () -> Unit,
        val tone: ContentTone = ContentTone.Inherit,
        val enabled: Boolean = true,
        val badgeCount: Int? = null,
        /** A dropdown anchored to this action's button; the caller owns its expanded state. */
        val menu: (@Composable () -> Unit)? = null,
    ) : AppBarAction

    data class OverflowAction(
        val title: String,
        val onClick: () -> Unit,
        val icon: ImageVector? = null,
    ) : AppBarAction
}
