package mihon.presentation.kit.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

/** The dot beside a legend's label, in the [color] its series is drawn with. */
@Composable
fun LegendSwatch(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(SWATCH_SIZE).background(color, KatariTheme.shapes.full))
}

private val SWATCH_SIZE = 10.dp
