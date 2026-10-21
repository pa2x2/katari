package eu.kanade.presentation.updates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.entry.entryTypePresentation
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FilterListOff
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.EmptyScreenAction
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.updates.model.UpdatesFeedFilter
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * "Unseen · Manga · not Dropped": every filter in use, named the way the filter sheet names it.
 *
 * @param sources source names by id.
 */
@Composable
fun updatesFeedFilterSummary(
    filter: UpdatesFeedFilter,
    categories: List<Category>,
    sources: Map<Long, String>,
): String {
    val categoryNames = categories.associate { it.id to it.visualName }
    val parts = buildList {
        listOf(
            filter.downloaded to stringResource(MR.strings.label_downloaded),
            filter.unseen to stringResource(MR.strings.action_filter_unseen),
            filter.started to stringResource(MR.strings.label_started),
            filter.bookmarked to stringResource(MR.strings.action_filter_bookmarked),
        ).forEach { (state, label) ->
            when (state) {
                TriState.ENABLED_IS -> add(label)
                TriState.ENABLED_NOT -> add(stringResource(MR.strings.updates_filter_not, label))
                TriState.DISABLED -> Unit
            }
        }
        addSelection(filter.categories) { categoryNames[it] }
        addSelection(filter.types) { stringResource(it.entryTypePresentation().displayNameLabel) }
        addSelection(filter.sources) { sources[it] }
    }
    return parts.joinToString(" · ")
}

/** Names that can't be resolved, such as a source that is no longer installed, are left out of the summary. */
@Composable
private fun <T> MutableList<String>.addSelection(
    selection: UpdatesFeedFilter.Selection<T>,
    name: @Composable (T) -> String?,
) {
    selection.included.forEach { value -> name(value)?.let(::add) }
    selection.excluded.forEach { value ->
        name(value)?.let { add(stringResource(MR.strings.updates_filter_not, it)) }
    }
}

/** Keeps the filters in view, under the app bar, while they hide part of the feed; tapping it opens the filter sheet. */
@Composable
fun UpdatesFilterSummaryBar(
    summary: String,
    hiddenByFilters: Int,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.xs),
        container = SurfaceContainer.SecondaryContainer,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(start = KatariTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = listOf(
                    summary,
                    pluralStringResource(MR.plurals.updates_hidden, hiddenByFilters, hiddenByFilters),
                ).joinToString(" — "),
                modifier = Modifier.weight(1f),
                role = TextRole.Label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Button(
                label = stringResource(MR.strings.action_clear),
                onClick = onClear,
                style = ButtonStyle.Text,
            )
        }
    }
}

/**
 * Ends the feed with what it doesn't show: updates the filters hide, updates from hidden sources, and anything older
 * than the window.
 */
internal fun LazyListScope.updatesFeedFooterItem(
    hiddenByFilters: Int,
    fromHiddenSources: Int,
    onClearFilters: () -> Unit,
) {
    item(key = "updates-footer") {
        Column(
            modifier = Modifier
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                .fillMaxWidth()
                .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
        ) {
            if (hiddenByFilters > 0) {
                Text(
                    text = listOf(
                        pluralStringResource(MR.plurals.updates_hidden_by_filters, hiddenByFilters, hiddenByFilters),
                        stringResource(MR.strings.action_clear),
                    ).joinToString(" · "),
                    modifier = Modifier
                        .clickable(onClick = onClearFilters)
                        .padding(vertical = KatariTheme.spacing.xs),
                    role = TextRole.Supporting,
                    tone = ContentTone.Primary,
                )
            }
            if (fromHiddenSources > 0) {
                Text(
                    text = pluralStringResource(
                        MR.plurals.updates_from_hidden_sources,
                        fromHiddenSources,
                        fromHiddenSources,
                    ),
                    modifier = Modifier.padding(vertical = KatariTheme.spacing.xs),
                    role = TextRole.Supporting,
                )
            }
            Text(
                text = stringResource(MR.strings.updates_window),
                modifier = Modifier.padding(vertical = KatariTheme.spacing.xs),
                role = TextRole.Supporting,
            )
        }
    }
}

/** Says which filters emptied the feed, rather than that there are no updates. */
@Composable
fun UpdatesFilteredEmptyScreen(
    summary: String,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyScreen(
        message = stringResource(MR.strings.updates_no_match, summary),
        modifier = modifier,
        actions = listOf(
            EmptyScreenAction(
                stringRes = MR.strings.action_clear_filters,
                icon = MaterialSymbols.Rounded.FilterListOff,
                onClick = onClearFilters,
            ),
        ),
    )
}
