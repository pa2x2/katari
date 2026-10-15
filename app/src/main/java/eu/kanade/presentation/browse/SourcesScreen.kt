package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
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
import mihon.icons.materialsymbols.roundedfilled.Keep
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.list.SettingsRow
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
    onLongClickItem: (Source) -> Unit,
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

                items(
                    items = state.items,
                    contentType = {
                        when (it) {
                            is SourceUiModel.Header -> "header"
                            is SourceUiModel.Item -> "item"
                        }
                    },
                    key = {
                        when (it) {
                            is SourceUiModel.Header -> it.hashCode()
                            is SourceUiModel.Item -> "source-${it.source.key()}"
                        }
                    },
                ) { model ->
                    when (model) {
                        is SourceUiModel.Header -> {
                            SourceHeader(
                                modifier = Modifier.animateItem(),
                                language = model.language,
                            )
                        }
                        is SourceUiModel.Item -> SourceItem(
                            modifier = Modifier.animateItem(),
                            source = model.source,
                            onClickItem = onClickItem,
                            onLongClickItem = onLongClickItem,
                            onClickPin = onClickPin,
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
    val context = LocalContext.current
    SectionHeader(
        text = LocaleHelper.getSourceDisplayName(language, context),
        modifier = modifier,
    )
}

@Composable
private fun SourceItem(
    source: Source,
    onClickItem: (Source, SourceListListing) -> Unit,
    onLongClickItem: (Source) -> Unit,
    onClickPin: (Source) -> Unit,
    itemClicksEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        source = source,
        onClickItem = {
            if (itemClicksEnabled) {
                onClickItem(source, SourceListListing.Popular)
            }
        },
        onLongClickItem = { onLongClickItem(source) },
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
fun SourceOptionsDialog(
    source: Source,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        title = source.visualName,
        confirm = null,
    ) {
        Column {
            val textId = if (Pin.Pinned in source.pin) MR.strings.action_unpin else MR.strings.action_pin
            SettingsRow(title = stringResource(textId), onClick = onClickPin)
            if (!source.isLocal()) {
                SettingsRow(title = stringResource(MR.strings.action_disable), onClick = onClickDisable)
            }
        }
    }
}

sealed interface SourceUiModel {
    data class Item(val source: Source) : SourceUiModel
    data class Header(val language: String) : SourceUiModel
}
