package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

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

@Composable
internal fun DatePickerCell(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    description: String = label,
    calendarDay: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        container = when {
            selected -> SurfaceContainer.Selected
            calendarDay -> SurfaceContainer.None
            else -> SurfaceContainer.Default
        },
        shape = if (calendarDay) KatariTheme.shapes.full else KatariTheme.shapes.medium,
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
