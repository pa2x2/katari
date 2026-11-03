package mihon.presentation.kit.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * A line across a list that marks a point in it. With [text] the line runs both sides of what lies there, such as how
 * many chapters are missing or where already seen items start; without, it is a short rule between two neighbours,
 * such as the chapters on a reader's transition page.
 */
@Composable
fun MarkerLine(text: String?, modifier: Modifier = Modifier, tone: ContentTone = ContentTone.Inherit) {
    if (text == null) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HorizontalDivider(Modifier.fillMaxWidth(SHORT_RULE_FRACTION))
        }
        return
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(Modifier.weight(1f))
        Text(text = text, role = TextRole.Supporting, tone = tone)
        HorizontalDivider(Modifier.weight(1f))
    }
}

private const val SHORT_RULE_FRACTION = 0.32f
