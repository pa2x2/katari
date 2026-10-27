package mihon.presentation.kit.content

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * A day of a calendar month: its number on a round cell with a dot per event below it, up to [EventDots]' limit. Today
 * is ringed and days already [past] are dimmed.
 */
@Composable
fun CalendarDayCell(
    day: String,
    today: Boolean,
    past: Boolean,
    events: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = KatariTheme.shapes.full
    Box(
        modifier = modifier
            .then(
                if (today) {
                    Modifier.border(KatariTheme.sizes.border, KatariTheme.colors.onBackground, shape)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .circleLayout(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day,
            role = TextRole.SectionTitle,
            tone = if (past) ContentTone.Disabled else ContentTone.Inherit,
            textAlign = TextAlign.Center,
        )
        EventDots(count = events, modifier = Modifier.offset(y = KatariTheme.spacing.m))
    }
}

/** Lays the cell out as a circle as wide as its widest side, so every day of the grid is the same round cell. */
private fun Modifier.circleLayout() = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val diameter = maxOf(placeable.height, placeable.width)
    layout(diameter, diameter) {
        placeable.placeRelative(x = (diameter - placeable.width) / 2, y = (diameter - placeable.height) / 2)
    }
}
