package mihon.presentation.kit.field

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** What a [PickerCell] picks, which sets its shape. */
enum class PickerCellKind {
    /** A day in a month's calendar, drawn round. */
    Day,

    /** A longer span, such as a month or a year, drawn as a tile. */
    Period,
}

/** One choice in a grid of dates to pick from, such as a day of a month or a year. */
@Composable
fun PickerCell(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    kind: PickerCellKind,
    modifier: Modifier = Modifier,
    description: String = label,
    onClick: () -> Unit,
) {
    val day = kind == PickerCellKind.Day
    Surface(
        modifier = modifier,
        container = when {
            selected -> SurfaceContainer.Selected
            day -> SurfaceContainer.None
            else -> SurfaceContainer.Default
        },
        shape = if (day) KatariTheme.shapes.full else KatariTheme.shapes.medium,
    ) {
        Box(
            Modifier
                .selectable(selected, enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }
                .heightIn(min = KatariTheme.sizes.touchTarget)
                .padding(horizontal = KatariTheme.spacing.xs, vertical = KatariTheme.spacing.s),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                role = TextRole.ItemTitle,
                tone = if (enabled) ContentTone.Inherit else ContentTone.Disabled,
                textAlign = TextAlign.Center,
            )
        }
    }
}
