package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.displayName
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.source.model.Source

/**
 * How a feed is named wherever it is shown. [title] is the user's name for the feed or its preset name, and
 * [subtitle] says where it comes from, with the source language so per-language copies of a source can be told apart.
 */
@Immutable
internal data class FeedLabel(
    val title: String,
    val subtitle: String,
)

internal fun feedLabel(
    customTitle: String?,
    presetName: String,
    sourceName: String,
    languageName: String,
): FeedLabel {
    val origin = listOf(sourceName, languageName).filter { it.isNotBlank() }
    return if (customTitle.isNullOrBlank()) {
        FeedLabel(title = presetName, subtitle = origin.joinToString(LABEL_SEPARATOR))
    } else {
        // The preset goes last so that a truncated subtitle loses it before the language.
        FeedLabel(title = customTitle, subtitle = (origin + presetName).joinToString(LABEL_SEPARATOR))
    }
}

@Composable
internal fun rememberFeedLabel(feed: SourceFeed, source: Source, preset: SourceFeedPreset): FeedLabel {
    val context = LocalContext.current
    val presetName = preset.displayName()
    return remember(feed.title, presetName, source.name, source.lang) {
        feedLabel(
            customTitle = feed.title,
            presetName = presetName,
            sourceName = source.name,
            languageName = LocaleHelper.getSourceDisplayName(source.lang, context),
        )
    }
}

@Composable
internal fun FeedLabelText(
    label: FeedLabel,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(modifier = modifier) {
        Text(
            text = label.title,
            style = titleStyle,
            color = LocalContentColor.current,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = subtitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val LABEL_SEPARATOR = " · "
