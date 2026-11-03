package mihon.presentation.kit.content

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import mihon.presentation.kit.theme.KatariTheme

/** Clips a bar, a share bar or a progress track of a chart to a pill. */
@Composable
fun Modifier.chartBar(): Modifier = clip(KatariTheme.shapes.full)

/** Clips a calendar day, a legend swatch or a column of a bar chart to its slightly rounded square. */
@Composable
fun Modifier.chartCell(): Modifier = clip(KatariTheme.shapes.extraSmall)

/** Outlines a legend sample drawn without a fill of its own, such as the hatched square explaining hatching. */
@Composable
fun Modifier.chartLegendSample(): Modifier =
    border(KatariTheme.sizes.border, KatariTheme.chart.outline, KatariTheme.shapes.extraSmall)
