package mihon.feature.upcoming.components.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mihon.presentation.kit.content.EventDots
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import kotlin.time.Clock

@Composable
fun CalendarDay(
    date: LocalDate,
    events: Int,
    onDayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val shape = KatariTheme.shapes.full

    Box(
        modifier = modifier
            .then(
                if (today == date) {
                    Modifier.border(KatariTheme.sizes.border, KatariTheme.colors.onBackground, shape)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .clickable(onClick = onDayClick)
            .circleLayout(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.day.toString(),
            role = TextRole.SectionTitle,
            tone = if (date < today) ContentTone.Disabled else ContentTone.Inherit,
            textAlign = TextAlign.Center,
        )
        EventDots(count = events, modifier = Modifier.offset(y = KatariTheme.spacing.m))
    }
}

private fun Modifier.circleLayout() = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    val currentHeight = placeable.height
    val currentWidth = placeable.width
    val newDiameter = maxOf(currentHeight, currentWidth)

    layout(newDiameter, newDiameter) {
        placeable.placeRelative(
            x = (newDiameter - currentWidth) / 2,
            y = (newDiameter - currentHeight) / 2,
        )
    }
}
