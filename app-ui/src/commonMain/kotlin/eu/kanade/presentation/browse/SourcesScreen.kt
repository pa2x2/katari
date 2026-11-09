package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.katari.core.platform.currentPlatformContext
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Keep
import app.katari.icons.materialsymbols.rounded.VisibilityOff
import app.katari.icons.materialsymbols.roundedfilled.Keep
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.field.SearchField
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.layout.RowListHeader
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.rowGroupItems
import app.katari.presentation.kit.menu.DropdownMenu
import app.katari.presentation.kit.menu.DropdownMenuItem
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.domain.source.interactor.SourceListListing
import eu.kanade.domain.source.interactor.SourceListState
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.browse.components.ContentTypeFilterSummary
import eu.kanade.tachiyomi.ui.browse.ContentTypeFilter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

@Composable
fun SourcesScreen(
    state: SourceListState,
    contentTypeFilter: ContentTypeFilter,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    contentPadding: PaddingValues,
    onClickItem: (Source, SourceListListing) -> Unit,
    onClickPin: (Source) -> Unit,
    onClickDisable: (Source) -> Unit,
    onOpenFilter: () -> Unit,
    itemClicksEnabled: Boolean = true,
) {
    Column {
        if (state.isLoading || state.hasSources) {
            RowListHeader(wrapGroups = true) {
                SearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = stringResource(MR.strings.source_find_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
                )
            }
        }
        SourceList(
            state = state,
            contentTypeFilter = contentTypeFilter,
            contentPadding = contentPadding,
            onClickItem = onClickItem,
            onClickPin = onClickPin,
            onClickDisable = onClickDisable,
            onOpenFilter = onOpenFilter,
            itemClicksEnabled = itemClicksEnabled,
        )
    }
}

@Composable
private fun SourceList(
    state: SourceListState,
    contentTypeFilter: ContentTypeFilter,
    contentPadding: PaddingValues,
    onClickItem: (Source, SourceListListing) -> Unit,
    onClickPin: (Source) -> Unit,
    onClickDisable: (Source) -> Unit,
    onOpenFilter: () -> Unit,
    itemClicksEnabled: Boolean,
) {
    when {
        state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
        state.isEmpty -> EmptyScreen(
            stringRes = if (state.hasSources || contentTypeFilter.isActive) {
                MR.strings.no_results_found
            } else {
                MR.strings.source_empty_screen
            },
            modifier = Modifier.padding(contentPadding),
        )
        else -> {
            RowList(
                contentPadding = contentPadding + PaddingValues(top = KatariTheme.spacing.s),
                wrapGroups = true,
            ) {
                if (contentTypeFilter.isActive) {
                    item(key = "source-content-type-filter-summary") {
                        ContentTypeFilterSummary(filter = contentTypeFilter, onClick = onOpenFilter)
                    }
                }

                state.groups.forEach { group ->
                    item(key = "source-header-${group.language}", contentType = "header") {
                        SourceHeader(
                            modifier = Modifier.animateItem(),
                            language = group.language,
                        )
                    }
                    rowGroupItems(
                        items = group.sources,
                        key = { "source-${it.key()}" },
                        contentType = { "item" },
                    ) { source ->
                        SourceItem(
                            source = source,
                            onClickItem = onClickItem,
                            onClickPin = onClickPin,
                            onClickDisable = onClickDisable,
                            itemClicksEnabled = itemClicksEnabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceHeader(
    language: String,
    modifier: Modifier = Modifier,
) {
    val context = currentPlatformContext()
    RowGroupHeader(
        text = LocaleHelper.getSourceDisplayName(language, context),
        modifier = modifier,
    )
}

/** A source's row; a long press opens a menu at the row to pin or disable it. */
@Composable
private fun SourceItem(
    source: Source,
    onClickItem: (Source, SourceListListing) -> Unit,
    onClickPin: (Source) -> Unit,
    onClickDisable: (Source) -> Unit,
    itemClicksEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        BaseSourceItem(
            source = source,
            onClickItem = {
                if (itemClicksEnabled) {
                    onClickItem(source, SourceListListing.Popular)
                }
            },
            onLongClickItem = { menuExpanded = true },
            action = {
                if (itemClicksEnabled && source.catalogue?.supportsLatest == true) {
                    Button(
                        label = stringResource(MR.strings.latest),
                        onClick = { onClickItem(source, SourceListListing.Latest) },
                        style = ButtonStyle.Text,
                    )
                }
                SourcePinButton(
                    isPinned = Pin.Pinned in source.pin,
                    onClick = { onClickPin(source) },
                )
            },
        )
        SourceOptionsMenu(
            source = source,
            expanded = menuExpanded,
            onClickPin = { onClickPin(source) },
            onClickDisable = { onClickDisable(source) },
            onDismissRequest = { menuExpanded = false },
        )
    }
}

@Composable
private fun SourcePinButton(
    isPinned: Boolean,
    onClick: () -> Unit,
) {
    val description = if (isPinned) MR.strings.action_unpin else MR.strings.action_pin
    IconButton(
        icon = if (isPinned) MaterialSymbols.RoundedFilled.Keep else MaterialSymbols.Rounded.Keep,
        contentDescription = stringResource(description),
        onClick = onClick,
        tone = if (isPinned) ContentTone.Primary else ContentTone.Secondary,
    )
}

@Composable
private fun SourceOptionsMenu(
    source: Source,
    expanded: Boolean,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        val isPinned = Pin.Pinned in source.pin
        DropdownMenuItem(
            label = stringResource(if (isPinned) MR.strings.action_unpin else MR.strings.action_pin),
            onClick = onClickPin,
            icon = if (isPinned) MaterialSymbols.RoundedFilled.Keep else MaterialSymbols.Rounded.Keep,
        )
        if (source.id != LOCAL_SOURCE_ID) {
            DropdownMenuItem(
                label = stringResource(MR.strings.action_disable),
                onClick = onClickDisable,
                icon = MaterialSymbols.Rounded.VisibilityOff,
            )
        }
    }
}
