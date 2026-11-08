package app.katari.presentation.kit.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.MoreVert
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.menu.DropdownMenu
import app.katari.presentation.kit.menu.DropdownMenuDivider
import app.katari.presentation.kit.menu.DropdownMenuItem
import app.katari.presentation.kit.menu.OverflowMenu
import app.katari.presentation.kit.selection.SelectionActions
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.presentation.kit.window.WindowWidth
import app.katari.presentation.kit.window.currentWindowLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds

/**
 * The bar of actions on a selection. In a narrow window it is raised from the bottom edge above the navigation bar and
 * shares its width between the actions; from a medium one it floats as a pill centred over the content, only as wide
 * as its actions, so they don't spread across the screen. Fill it with [SelectionAction]s; show and hide it with the
 * selection.
 */
@Composable
fun SelectionActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    if (currentWindowLayout().width == WindowWidth.Compact) {
        Surface(
            modifier = modifier,
            container = SurfaceContainer.High,
            shape = KatariTheme.shapes.extraLargeTop,
        ) {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(horizontal = KatariTheme.spacing.s, vertical = KatariTheme.spacing.m),
                content = content,
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                )
                // Centred over the content rather than the window when a side dock floats beside it
                .padding(start = LocalDockClearance.current.start)
                .padding(KatariTheme.spacing.m),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                container = SurfaceContainer.High,
                shape = KatariTheme.shapes.full,
                shadow = true,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
                ) {
                    CompositionLocalProvider(LocalFloatingSelectionBar provides true) { content() }
                }
            }
        }
    }
}

/** How a [SelectionActionBar] captions its actions. */
enum class SelectionActionLabels {
    /** Every action is captioned, and More opens the rest. */
    Shown,

    /** Icons only, with More as an overflow button; a long press reveals an action's caption for a moment. */
    OnLongPress,
}

/**
 * A [SelectionActionBar] of [actions]: each of [SelectionActions.actions] as a [SelectionAction], one that has choices
 * opening a menu of them, and [SelectionActions.overflow] behind More.
 */
@Composable
fun SelectionActionBar(
    actions: SelectionActions,
    modifier: Modifier = Modifier,
    labels: SelectionActionLabels = SelectionActionLabels.OnLongPress,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var revealed by remember { mutableStateOf<Int?>(null) }
    var hideLabel by remember { mutableStateOf<Job?>(null) }
    val revealLabel = { index: Int ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        revealed = index
        hideLabel?.cancel()
        hideLabel = scope.launch {
            delay(REVEALED_LABEL_DURATION)
            if (revealed == index) revealed = null
        }
    }
    SelectionActionBar(modifier = modifier) {
        actions.actions.forEachIndexed { index, action ->
            var choicesShown by remember { mutableStateOf(false) }
            val toggleChoices = { choicesShown = !choicesShown }
            val onLongClick: () -> Unit = if (labels == SelectionActionLabels.OnLongPress) {
                { revealLabel(index) }
            } else {
                // Held down, a captioned action does nothing rather than acting as a tap on release
                {}
            }
            SelectionAction(
                title = action.label,
                icon = action.icon,
                onClick = if (action.choices.isEmpty()) action.onClick else toggleChoices,
                showLabel = labels == SelectionActionLabels.Shown,
                revealLabel = revealed == index,
                onLongClick = onLongClick,
                menu = if (action.choices.isEmpty()) {
                    null
                } else {
                    {
                        DropdownMenu(expanded = choicesShown, onDismissRequest = { choicesShown = false }) {
                            action.choices.forEach { choice ->
                                DropdownMenuItem(label = choice.label, onClick = choice.onClick, icon = choice.icon)
                            }
                        }
                    }
                },
            )
        }
        val overflow = actions.overflow.filter { it.isNotEmpty() }
        if (overflow.isNotEmpty()) {
            val overflowItems: @Composable ColumnScope.() -> Unit = {
                overflow.forEachIndexed { index, group ->
                    if (index > 0) DropdownMenuDivider()
                    group.forEach { DropdownMenuItem(label = it.label, onClick = it.onClick, icon = it.icon) }
                }
            }
            when (labels) {
                SelectionActionLabels.Shown -> {
                    var overflowShown by remember { mutableStateOf(false) }
                    SelectionAction(
                        title = stringResource(MR.strings.label_more),
                        icon = MaterialSymbols.Rounded.MoreVert,
                        onClick = { overflowShown = true },
                        showLabel = true,
                        onLongClick = {},
                        menu = {
                            DropdownMenu(
                                expanded = overflowShown,
                                onDismissRequest = { overflowShown = false },
                                content = overflowItems,
                            )
                        },
                    )
                }
                SelectionActionLabels.OnLongPress -> OverflowMenu { overflowItems() }
            }
        }
    }
}

/**
 * One action of a [SelectionActionBar]; the bar shares its width between them. With [showLabel] the action is
 * captioned; otherwise only [revealLabel] shows the caption, widening the action, such as briefly after a long press
 * tells what an icon does. [menu] holds a dropdown anchored to the action.
 */
@Composable
fun RowScope.SelectionAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
    revealLabel: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    menu: (@Composable () -> Unit)? = null,
) {
    val weight by animateFloatAsState(
        targetValue = if (revealLabel) REVEALED_WEIGHT else 1f,
        label = "weight",
    )
    // A floating bar is only as wide as its actions, so each takes a fixed width rather than a share
    val share = if (LocalFloatingSelectionBar.current) {
        Modifier.width(FloatingActionWidth * weight)
    } else {
        Modifier.weight(weight)
    }
    Box(
        modifier = modifier
            .then(
                if (showLabel) {
                    Modifier.heightIn(min = KatariTheme.sizes.touchTarget)
                } else {
                    Modifier.size(KatariTheme.sizes.touchTarget)
                },
            )
            .then(share)
            .combinedClickable(
                interactionSource = null,
                indication = ripple(bounded = false),
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(imageVector = icon, contentDescription = title)
            if (showLabel) {
                Text(
                    text = title,
                    modifier = Modifier.padding(
                        top = KatariTheme.spacing.xs,
                        start = KatariTheme.spacing.xxs,
                        end = KatariTheme.spacing.xxs,
                    ),
                    role = TextRole.Badge,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                )
            } else {
                AnimatedVisibility(
                    visible = revealLabel,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    Text(text = title, role = TextRole.Badge, overflow = TextOverflow.Visible, maxLines = 1)
                }
            }
        }
        menu?.invoke()
    }
}

private const val REVEALED_WEIGHT = 2f

private val REVEALED_LABEL_DURATION = 1.seconds

private val FloatingActionWidth = 88.dp

private val LocalFloatingSelectionBar = staticCompositionLocalOf { false }
