package app.katari.presentation.kit.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.MoreVert
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.content.Spin
import app.katari.presentation.kit.feedback.Tooltip
import app.katari.presentation.kit.menu.DropdownMenu
import app.katari.presentation.kit.menu.DropdownMenuDivider
import app.katari.presentation.kit.menu.DropdownMenuItem
import app.katari.presentation.kit.sheet.Popover
import app.katari.presentation.kit.sheet.PopoverAnchor
import app.katari.presentation.kit.sheet.popoverAnchor
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.shortcut.KeyboardShortcutHandler
import app.katari.presentation.kit.theme.ContentTone
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Icon actions first, each with a tooltip that names its shortcut; overflow actions collect in one "More" menu at the
 * end. An action's shortcut runs it while the actions are shown.
 */
@Composable
fun AppBarActions(actions: List<AppBar.AppBarAction>) {
    actions.forEach { action ->
        when (action) {
            is AppBar.Action -> action.shortcut?.let {
                KeyboardShortcutHandler(it, enabled = action.enabled, onTrigger = action.onClick)
            }
            is AppBar.OverflowAction -> action.shortcut?.let {
                KeyboardShortcutHandler(it, onTrigger = action.onClick)
            }
        }
    }

    // A menu anchors to its parent layout, so each one shares a Box with its button rather than anchoring to the
    // whole actions row.
    actions.filterIsInstance<AppBar.Action>().forEach { action ->
        Box(modifier = action.anchor?.let { Modifier.popoverAnchor(it) } ?: Modifier) {
            ActionButton(action)
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
                overflowActions.forEachIndexed { index, action ->
                    if (index > 0 && overflowActions[index - 1].group != action.group) DropdownMenuDivider()
                    DropdownMenuItem(label = action.title, onClick = action.onClick, icon = action.icon)
                }
            }
        }
    }
}

/**
 * [action] as an icon button with a tooltip naming it and its shortcut, wherever it shows: an app bar or the dock. An
 * action with a long press of its own shows the tooltip only on hover, since a touch long press would show it too.
 */
@Composable
internal fun ActionButton(action: AppBar.Action) {
    val button: @Composable () -> Unit = {
        IconButton(
            icon = action.icon,
            contentDescription = action.title,
            onClick = action.onClick,
            tone = action.tone,
            enabled = action.enabled,
            badgeCount = action.badgeCount,
            shortcut = action.shortcut,
            spin = action.spin,
            arrows = action.arrows,
            onLongClick = action.longPress?.onLongClick,
            onLongClickLabel = action.longPress?.label,
        )
    }
    if (action.longPress != null) button() else Tooltip(action.title, shortcut = action.shortcut, content = button)
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
        /** Turns the icon, the way its arrows point, while the work it stands for runs; null keeps it still. */
        val spin: Spin? = null,
        /** Arrows drawn over [icon] that turn alone while [spin] is set, such as sync's inside its cloud. */
        val arrows: ImageVector? = null,
        /** A dropdown anchored to this action's button; the caller owns its expanded state. */
        val menu: (@Composable () -> Unit)? = null,
        val shortcut: KeyboardShortcut? = null,
        val longPress: LongPress? = null,
        /** Marks this action's button for a [Popover] it opens, such as one a choice in its [menu] leads to. */
        val anchor: PopoverAnchor? = null,
    ) : AppBarAction

    /**
     * What a long press of an [Action]'s button does besides its tap, such as sync's status icon syncing at once;
     * [label] names it to accessibility services.
     */
    data class LongPress(val label: String, val onLongClick: () -> Unit)

    /**
     * An action in the app bar's overflow menu. Consecutive actions with the same [group] sit together, and a break
     * separates them from the next group, so related actions read as one set however many of them a screen shows.
     */
    data class OverflowAction(
        val title: String,
        val onClick: () -> Unit,
        val icon: ImageVector,
        val group: Int = 0,
        val shortcut: KeyboardShortcut? = null,
    ) : AppBarAction
}
