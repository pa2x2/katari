package app.katari.presentation.kit.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonSize
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.theme.KatariTheme

/**
 * One of a screen's main actions, such as Restore or Save as Katari backup, which the [Scaffold] places: in a bar at
 * the bottom, or at the end of the app bar in a short window. A screen lists them with the main one last.
 */
@Immutable
class ScreenAction(
    val label: String,
    val onClick: () -> Unit,
    val style: ButtonStyle = ButtonStyle.Filled,
    val enabled: Boolean = true,
    val loading: Boolean = false,
)

/**
 * The screen's main actions, provided to its top bar in a short window, where the app bar shows them at its end
 * instead of the bottom bar taking a quarter of the height.
 */
internal val LocalScreenActions = staticCompositionLocalOf<List<ScreenAction>> { emptyList() }

/**
 * [actions] side by side, sharing the width equally; stacked, the main action on top, when a label wouldn't fit on one
 * line in its share, such as with a large font.
 */
@Composable
internal fun ActionButtons(actions: List<ScreenAction>, modifier: Modifier = Modifier) {
    val gap = KatariTheme.spacing.s
    Layout(
        content = {
            actions.forEach { action ->
                Button(
                    label = action.label,
                    onClick = action.onClick,
                    style = action.style,
                    enabled = action.enabled,
                    loading = action.loading,
                )
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        if (measurables.isEmpty()) return@Layout layout(0, 0) {}
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val share = (width - gapPx * (measurables.size - 1)) / measurables.size
        val fits = measurables.all { it.maxIntrinsicWidth(Constraints.Infinity) <= share }
        if (fits) {
            val placeables = measurables.map { it.measure(Constraints.fixedWidth(share)) }
            layout(width, placeables.maxOf { it.height }) {
                placeables.forEachIndexed { index, placeable -> placeable.place(index * (share + gapPx), 0) }
            }
        } else {
            val placeables = measurables.asReversed().map { it.measure(Constraints.fixedWidth(width)) }
            layout(width, placeables.sumOf { it.height } + gapPx * (placeables.size - 1)) {
                var y = 0
                placeables.forEach { placeable ->
                    placeable.place(0, y)
                    y += placeable.height + gapPx
                }
            }
        }
    }
}

/** [actions] at the end of an app bar: small, with an outlined one shown as text, as the bar's other actions are. */
@Composable
internal fun AppBarScreenActions(actions: List<ScreenAction>) {
    Row(
        // The bar's actions end 4dp from its edge, which an icon button's own inset hides; a filled button there would
        // reach 8dp past the content's edge, so the row ends that much further in
        modifier = Modifier.padding(end = KatariTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { action ->
            Button(
                label = action.label,
                onClick = action.onClick,
                style = if (action.style == ButtonStyle.Outlined) ButtonStyle.Text else action.style,
                enabled = action.enabled,
                loading = action.loading,
                size = ButtonSize.Small,
            )
        }
    }
}
