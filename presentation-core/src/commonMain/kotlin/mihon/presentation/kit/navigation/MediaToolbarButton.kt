package mihon.presentation.kit.navigation

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import mihon.presentation.kit.button.ContentButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * A tool button over media with its [label] after its icon, for a [MediaToolbar]. Its minimum intrinsic width is the
 * icon's alone, and measured narrower than the whole it leaves the label out, keeping its icon and [iconDescription]
 * rather than an ellipsis.
 */
@Composable
fun MediaToolbarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconDescription: String,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    val gap = spacing.s
    ContentButton(onClick = onClick, modifier = modifier.heightIn(min = KatariTheme.sizes.touchTargetLarge)) {
        Layout(
            modifier = Modifier.padding(horizontal = spacing.m),
            content = {
                Icon(imageVector = icon, contentDescription = iconDescription, tone = ContentTone.OnMedia)
                Text(text = label, role = TextRole.Label, tone = ContentTone.OnMedia, maxLines = 1)
            },
            measurePolicy = remember(gap) { IconLabelMeasurePolicy(gap) },
        )
    }
}

private class IconLabelMeasurePolicy(private val gap: Dp) : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val gapPx = gap.roundToPx()
        val iconPlaceable = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val labelWidth = measurables[1].maxIntrinsicWidth(constraints.maxHeight)
        val labelPlaceable = measurables[1]
            .takeIf { iconPlaceable.width + gapPx + labelWidth <= constraints.maxWidth }
            ?.measure(Constraints(maxWidth = labelWidth, maxHeight = constraints.maxHeight))
        val contentWidth = iconPlaceable.width + (labelPlaceable?.let { gapPx + it.width } ?: 0)
        val width = contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = maxOf(iconPlaceable.height, labelPlaceable?.height ?: 0)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(width, height) {
            val start = (width - contentWidth) / 2
            iconPlaceable.placeRelative(start, (height - iconPlaceable.height) / 2)
            labelPlaceable?.placeRelative(start + iconPlaceable.width + gapPx, (height - labelPlaceable.height) / 2)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables[0].minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables[0].maxIntrinsicWidth(height) + gap.roundToPx() + measurables[1].maxIntrinsicWidth(height)
}
