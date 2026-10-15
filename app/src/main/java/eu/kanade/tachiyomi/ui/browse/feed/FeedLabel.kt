package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.displayName
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
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

/**
 * A null [titleRole] takes the enclosing component's text style, such as a menu item's. [subtitleTone] lets a label on
 * a coloured container keep the container's content colour.
 */
@Composable
internal fun FeedLabelText(
    label: FeedLabel,
    modifier: Modifier = Modifier,
    titleRole: TextRole? = TextRole.ItemTitle,
    subtitleTone: ContentTone = ContentTone.Secondary,
) {
    Column(modifier = modifier) {
        Text(
            text = label.title,
            role = titleRole,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label.subtitle,
            role = TextRole.Supporting,
            tone = subtitleTone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val LABEL_SEPARATOR = " · "
