package mihon.presentation.kit.navigation

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import mihon.presentation.kit.button.ContentButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * Where playback is, as a button in a [MediaToolbar], such as a player's "6:12 / 14:48" beside its episode buttons;
 * [onClick], announced as [onClickLabel], switches what it shows, such as to the time left. Its minimum intrinsic
 * width is [compactText]'s, such as "6:12", which it shows when measured narrower than [text], so a
 * [labelDropOrder][mediaToolbarSlot] after the buttons' makes it shorten once their labels are gone.
 */
@Composable
fun MediaToolbarPosition(
    text: String,
    compactText: String,
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
) {
    ContentButton(
        onClick = onClick,
        // Outer semantics apply after the button's own click semantics, so this replaces its unlabelled action.
        modifier = modifier
            .heightIn(min = KatariTheme.sizes.touchTargetLarge)
            .semantics {
                onClick(label = onClickLabel) {
                    onClick()
                    true
                }
            },
    ) {
        Layout(
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.s),
            content = {
                Text(text = text, role = TextRole.Label, tone = ContentTone.OnMedia, maxLines = 1)
                Text(text = compactText, role = TextRole.Label, tone = ContentTone.OnMedia, maxLines = 1)
            },
            measurePolicy = FullOrCompactMeasurePolicy,
        )
    }
}

/** Shows the first child where it fits and the second one otherwise. */
private object FullOrCompactMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val fits = measurables[0].maxIntrinsicWidth(constraints.maxHeight) <= constraints.maxWidth
        val shown = measurables[if (fits) 0 else 1].measure(loose)
        val width = shown.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = shown.height.coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(width, height) {
            shown.placeRelative((width - shown.width) / 2, (height - shown.height) / 2)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables[1].maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables[0].maxIntrinsicWidth(height)
}
