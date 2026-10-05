package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.i18n.stringResource

/** Shared chapter-like row content for entry details and reader navigation surfaces. */
@Composable
fun EntryChildListItemContent(
    title: String,
    date: String?,
    readProgress: String?,
    scanlator: String?,
    read: Boolean?,
    bookmark: Boolean,
    unconsumedIndicatorLabel: StringResource = MR.strings.action_filter_unseen,
    modifier: Modifier = Modifier,
    /** Time recorded for the item, shown after [date]. */
    duration: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(modifier = modifier) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var textHeight by remember { mutableIntStateOf(0) }
                if (read == false) {
                    Icon(
                        imageVector = Icons.Filled.Circle,
                        contentDescription = stringResource(unconsumedIndicatorLabel),
                        modifier = Modifier
                            .height(8.dp)
                            .padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (bookmark) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                        modifier = Modifier
                            .sizeIn(maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp }),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textHeight = it.size.height },
                    color = LocalContentColor.current.copy(alpha = if (read == true) DISABLED_ALPHA else 1f),
                )
            }

            Row {
                val subtitleStyle = MaterialTheme.typography.bodySmall
                    .merge(
                        color = LocalContentColor.current
                            .copy(alpha = if (read == true) DISABLED_ALPHA else SECONDARY_ALPHA),
                    )
                ProvideTextStyle(value = subtitleStyle) {
                    val dimmed = LocalContentColor.current.copy(alpha = DISABLED_ALPHA)
                    val parts = listOfNotNull(
                        date?.let { it to Color.Unspecified },
                        duration?.let { it to Color.Unspecified },
                        readProgress?.let { it to dimmed },
                        scanlator?.let { it to Color.Unspecified },
                    )
                    parts.forEachIndexed { index, (text, color) ->
                        if (index > 0) DotSeparatorText()
                        Text(
                            text = text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = color,
                        )
                    }
                }
            }
        }

        trailingContent?.invoke()
    }
}
