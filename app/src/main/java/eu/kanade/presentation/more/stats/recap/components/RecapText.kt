package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/** Page text in the page's ink, entering at step [order]. */
@Composable
internal fun RecapText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalRecapPalette.current.ink,
    order: Int = 0,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier.recapReveal(order),
    )
}

/** The small line that names what a page is about, above its figure. */
@Composable
internal fun RecapKicker(text: String, modifier: Modifier = Modifier, order: Int = 0) {
    RecapText(
        text = text.uppercase(),
        style = RecapTypography.Kicker,
        color = LocalRecapPalette.current.accent,
        modifier = modifier,
        order = order,
    )
}
