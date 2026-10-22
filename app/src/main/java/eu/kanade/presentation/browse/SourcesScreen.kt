package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.source.interactor.SourceListListing
import eu.kanade.domain.source.interactor.SourceListState
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.browse.components.ContentTypeFilterSummary
import eu.kanade.tachiyomi.ui.browse.ContentTypeFilter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Keep
import mihon.icons.materialsymbols.rounded.VisibilityOff
import mihon.icons.materialsymbols.roundedfilled.Keep
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.source.local.isLocal

@Composable
fun SourcesScreen(
    state: SourceListState,
    contentTypeFilter: ContentTypeFilter,
    contentPadding: PaddingValues,
    onClickItem: (Source, SourceListListing) -> Unit,
    onClickPin: (Source) -> Unit,
    onClickDisable: (Source) -> Unit,
    itemClicksEnabled: Boolean = true,
) {
    when {
        state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
        state.isEmpty -> EmptyScreen(
            stringRes = if (contentTypeFilter.isActive) {
                MR.strings.no_results_found
            } else {
                MR.strings.source_empty_screen
            },
            modifier = Modifier.padding(contentPadding),
        )
        else -> {
            ScrollbarLazyColumn(
                contentPadding = contentPadding + PaddingValues(top = KatariTheme.spacing.s),
            ) {
                if (contentTypeFilter.isActive) {
                    item(key = "source-content-type-filter-summary") {
                        ContentTypeFilterSummary(filter = contentTypeFilter)
                    }
                }

                val positions = RowGroupPosition.ofRuns(state.items) { it is SourceUiModel.Item }
                itemsIndexed(
                    items = state.items,
                    contentType = { _, model ->
                        when (model) {
                            is SourceUiModel.Header -> "header"
                            is SourceUiModel.Item -> "item"
                        }
                    },
                    key = { _, model ->
                        when (model) {
                            is SourceUiModel.Header -> model.hashCode()
                            is SourceUiModel.Item -> "source-${model.source.key()}"
                        }
                    },
                ) { index, model ->
                    when (model) {
                        is SourceUiModel.Header -> {
                            SourceHeader(
                                modifier = Modifier.animateItem(),
                                language = model.language,
                            )
                        }
                        is SourceUiModel.Item -> RowGroupItem(
                            position = positions[index] ?: RowGroupPosition.Alone,
                            modifier = Modifier.animateItem(),
                        ) {
                            SourceItem(
                                source = model.source,
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
}

@Composable
private fun SourceHeader(
    language: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
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
        if (!source.isLocal()) {
            DropdownMenuItem(
                label = stringResource(MR.strings.action_disable),
                onClick = onClickDisable,
                icon = MaterialSymbols.Rounded.VisibilityOff,
            )
        }
    }
}

sealed interface SourceUiModel {
    data class Item(val source: Source) : SourceUiModel
    data class Header(val language: String) : SourceUiModel
}
