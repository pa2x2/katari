package app.katari.presentation.kit.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

/**
 * A part of a whole and how much of it the part took, as a compact row inside a row group card, such as an episode
 * watched that day and its time. [mark] is a small icon after the value that says something about the part, such as
 * that it was finished then, named by [markDescription]; the rows of a card keep the mark's room whether or not they
 * have one, so their values line up. A [heading] row names the rows under it instead, such as one session's time span
 * and its total above what it read. [underCover] rows continue the [CoverRow] above them and start where its lines do,
 * such as a title's episodes unfolded under it.
 */
@Composable
fun TallyRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    mark: ImageVector? = null,
    markDescription: String? = null,
    heading: Boolean = false,
    underCover: Boolean = false,
) {
    val spacing = KatariTheme.spacing
    val role = if (heading) TextRole.Supporting else TextRole.Body
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = KatariTheme.sizes.touchTarget - spacing.s)
            .padding(
                start = if (underCover) coverRowLinesStart() else LocalRowInset.current,
                end = LocalRowInset.current,
                top = spacing.xs,
                bottom = spacing.xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            role = role,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text = value, role = if (heading) TextRole.Supporting else TextRole.Label, maxLines = 1)
        Box(Modifier.size(KatariTheme.sizes.iconSmall), contentAlignment = Alignment.Center) {
            if (mark != null) {
                Icon(
                    imageVector = mark,
                    contentDescription = markDescription,
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                    tone = ContentTone.Primary,
                )
            }
        }
    }
}
