package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.more.stats.recap.motion.recapReveal
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole

/** Page text, in the page's ink unless a [tone] says otherwise, entering at step [order]. */
@Composable
internal fun RecapText(
    text: String,
    role: TextRole,
    modifier: Modifier = Modifier,
    tone: ContentTone = ContentTone.Inherit,
    order: Int = 0,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        role = role,
        tone = tone,
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
        role = TextRole.Label,
        tone = ContentTone.Primary,
        modifier = modifier,
        order = order,
    )
}
