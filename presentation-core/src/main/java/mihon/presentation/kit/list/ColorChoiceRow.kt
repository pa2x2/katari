package mihon.presentation.kit.list

import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import mihon.presentation.kit.theme.KatariTheme

/**
 * A titled set of colour swatches where one can be chosen, such as the colour of subtitle text. The [colors] are
 * values the setting offers, stored as ARGB integers, so they stay the same in every theme.
 */
@Composable
fun ColorChoiceRow(
    title: String,
    colors: List<Int>,
    @ColorInt selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionHeader(title)
        FlowRow(
            modifier = Modifier.padding(
                start = LocalRowInset.current,
                end = LocalRowInset.current,
                bottom = LocalRowDensity.current.verticalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            colors.forEach { color ->
                ColorSwatch(color = color, selected = color == selected, onClick = { onSelect(color) })
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    @ColorInt color: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = KatariTheme.shapes.medium
    val sizes = KatariTheme.sizes
    Box(
        modifier = Modifier
            .size(sizes.iconLarge)
            .clip(shape)
            .background(Color(color))
            .border(
                width = if (selected) sizes.selectedBorder else sizes.border,
                color = if (selected) KatariTheme.colors.primary else KatariTheme.colors.outlineVariant,
                shape = shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}
