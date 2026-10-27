package mihon.feature.migration.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import mihon.feature.migration.review.components.SourceMigrationEntryDetailsButton
import mihon.feature.migration.review.components.SourceMigrationEntryListItem
import mihon.feature.migration.review.components.sourceMigrationMatchKindLabel
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionItem
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Search
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeGroup
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun Screen.SourceMigrationCandidateSheet(
    sessionId: SourceMigrationSessionId,
    item: SourceMigrationSessionItem,
    onDismissRequest: () -> Unit,
) {
    val navigator = LocalNavigator.currentOrThrow
    val viewModel = assistedMetroViewModel<_, SourceMigrationCandidateSheetViewModel.Factory>(
        key = item.sourceEntryId.toString(),
    ) {
        create(sessionId, item.sourceEntryId)
    }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect {
            onDismissRequest()
        }
    }

    LaunchedEffect(state.detailsEntryId) {
        state.detailsEntryId?.let { entryId ->
            viewModel.consumeDetailsEntry()
            navigator.push(EntryScreen(entryId, fromSource = true))
        }
    }

    Sheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(title = stringResource(MR.strings.sourceMigrationReview_chooseReplacement))
            when {
                !state.isLoaded -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(KatariTheme.spacing.l),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator()
                    }
                }
                state.candidates.isNotEmpty() -> {
                    RowList(modifier = Modifier.weight(1f, fill = false)) {
                        itemsIndexed(
                            items = state.candidates,
                            key = { _, candidate ->
                                "${candidate.candidate.targetSourceId}:${candidate.candidate.targetUrl}"
                            },
                        ) { index, candidate ->
                            RowGroupItem(position = RowGroupPosition.of(index, state.candidates.size)) {
                                SourceMigrationCandidateRow(
                                    item = candidate,
                                    selected = candidate.candidate.targetSourceId == item.targetSourceId &&
                                        candidate.candidate.targetUrl == item.targetUrl,
                                    enabled = !state.isWorking,
                                    onClick = { viewModel.select(candidate.candidate) },
                                    onDetailsClick = { viewModel.openDetails(candidate.candidate) },
                                )
                            }
                        }
                    }
                }
            }
            if (state.failed) {
                RowGroupNote(
                    text = stringResource(MR.strings.sourceMigrationReview_replacementRejected),
                    tone = ContentTone.Error,
                )
            }
            Column(
                modifier = Modifier.padding(KatariTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            ) {
                if (item.selectedTargetEntryId != null) {
                    Button(
                        label = stringResource(MR.strings.sourceMigrationReview_clearReplacement),
                        onClick = viewModel::clearSelection,
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.Text,
                        enabled = !state.isWorking,
                    )
                }
                Button(
                    label = stringResource(MR.strings.sourceMigrationReview_searchAgain),
                    onClick = viewModel::searchAgain,
                    modifier = Modifier.fillMaxWidth(),
                    icon = MaterialSymbols.Rounded.Search,
                    enabled = !state.isWorking,
                    loading = state.isWorking,
                )
            }
        }
    }
}

@Composable
private fun SourceMigrationCandidateRow(
    item: SourceMigrationCandidateItem,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
) {
    SourceMigrationEntryListItem(
        title = item.candidate.targetTitle,
        thumbnailUrl = item.candidate.targetThumbnailUrl,
        onClick = onClick,
        enabled = enabled,
        supportingText = item.sourceName,
        supportingTrailing = {
            BadgeGroup {
                Badge(text = sourceMigrationMatchKindLabel(item.candidate.matchKind))
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Check,
                        contentDescription = stringResource(MR.strings.selected),
                        tone = ContentTone.Primary,
                    )
                }
                SourceMigrationEntryDetailsButton(
                    onClick = onDetailsClick,
                    enabled = enabled,
                )
            }
        },
    )
}
