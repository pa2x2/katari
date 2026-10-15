package mihon.presentation.kit.surface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import mihon.presentation.kit.theme.KatariTheme

/**
 * One choice of a set shown as a card, such as an engine in a picker. The kit owns the selected look: a primary
 * border and a tinted fill. A disabled card shows its content in the disabled colour, as disabled rows do.
 * [onLongClick] offers more about the choice, such as a menu to copy or open it.
 */
@Composable
fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = KatariTheme.colors
    val sizes = KatariTheme.sizes
    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onLongClick == null) {
                    Modifier.selectable(
                        selected = selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                        .semantics { this.selected = selected }
                        .combinedClickable(
                            enabled = enabled,
                            role = Role.RadioButton,
                            onLongClick = onLongClick,
                            onClick = onClick,
                        )
                },
            ),
        shape = KatariTheme.shapes.medium,
        color = if (selected) colors.secondaryContainer else colors.surface,
        contentColor = when {
            !enabled -> colors.disabledContent
            selected -> colors.onSecondaryContainer
            else -> colors.onSurface
        },
        border = if (selected) {
            BorderStroke(sizes.selectedBorder, colors.primary)
        } else {
            BorderStroke(sizes.border, colors.outlineVariant)
        },
    ) {
        Column(
            modifier = Modifier.padding(KatariTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            content = content,
        )
    }
}
