package app.katari.presentation.kit.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.pointer.clickCursor
import app.katari.presentation.kit.theme.KatariTheme

/**
 * A titled three by three grid of places over a picture where one can be chosen, such as where subtitles rest. The
 * places go by rows from the top left; [placeLabels] names each for screen readers, and [selected] is null while what
 * is placed sits elsewhere, such as subtitles dragged off their place. [actionLabel] beside the title runs [onAction],
 * such as a button putting them back.
 */
@Composable
fun PositionChoiceRow(
    title: String,
    placeLabels: List<String>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    require(placeLabels.size == PLACES) { "A position grid has $PLACES places" }
    Column(modifier) {
        SectionHeader(
            text = title,
            trailing = actionLabel?.let {
                { Button(label = it, onClick = onAction, style = ButtonStyle.Text) }
            },
        )
        Column(
            modifier = Modifier.padding(
                start = LocalRowInset.current,
                end = LocalRowInset.current,
                bottom = LocalRowDensity.current.verticalPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        ) {
            repeat(GRID_SIDE) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs)) {
                    repeat(GRID_SIDE) { column ->
                        val place = row * GRID_SIDE + column
                        PositionPlace(
                            label = placeLabels[place],
                            selected = place == selected,
                            onClick = { onSelect(place) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionPlace(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = KatariTheme.shapes.small
    val colors = KatariTheme.colors
    Box(
        modifier = Modifier
            .size(width = PLACE_WIDTH, height = PLACE_HEIGHT)
            .clip(shape)
            .background(if (selected) colors.secondaryContainer else colors.surfaceContainerHighest)
            .border(
                width = if (selected) KatariTheme.sizes.selectedBorder else KatariTheme.sizes.border,
                color = if (selected) colors.primary else colors.outlineVariant,
                shape = shape,
            )
            .clickCursor()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        // A short bar standing for a line of text at that place
        Box(
            Modifier
                .size(width = BAR_WIDTH, height = BAR_HEIGHT)
                .clip(KatariTheme.shapes.full)
                .background(if (selected) colors.onSecondaryContainer else colors.outline),
        )
    }
}

private const val GRID_SIDE = 3
private const val PLACES = GRID_SIDE * GRID_SIDE

private val PLACE_WIDTH = 44.dp
private val PLACE_HEIGHT = 28.dp
private val BAR_WIDTH = 18.dp
private val BAR_HEIGHT = 3.dp
