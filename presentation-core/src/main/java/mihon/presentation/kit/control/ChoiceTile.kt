package mihon.presentation.kit.control

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme

/** One choice of a setting shown as a picture of it, such as a schematic of a layout, beside the setting's other choices. */
@Composable
fun ChoiceTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = KatariTheme.colors
    val sizes = KatariTheme.sizes
    val shape = KatariTheme.shapes.medium
    Surface(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .border(
                width = if (selected) sizes.selectedBorder else sizes.border,
                color = if (selected) colors.primary else colors.outlineVariant,
                shape = shape,
            ),
        container = if (selected) SurfaceContainer.Selected else SurfaceContainer.Default,
        shape = shape,
    ) {
        Column(content = content)
    }
}
