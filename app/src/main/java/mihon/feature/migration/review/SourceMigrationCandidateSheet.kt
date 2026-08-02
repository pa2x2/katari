package mihon.feature.migration.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import mihon.feature.migration.review.components.SourceMigrationEntryDetailsButton
import mihon.feature.migration.review.components.SourceMigrationEntryListItem
import mihon.feature.migration.session.model.SourceMigrationMatchKind
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionItem
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.header

@Composable
internal fun Screen.SourceMigrationCandidateSheet(
    sessionId: SourceMigrationSessionId,
    item: SourceMigrationSessionItem,
    onDismissRequest: () -> Unit,
) {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel(tag = item.sourceEntryId.toString()) {
        SourceMigrationCandidateSheetModel(sessionId, item.sourceEntryId)
    }
    val state by screenModel.state.collectAsState()

    LaunchedEffect(Unit) {
        screenModel.events.collect {
            onDismissRequest()
        }
    }

    LaunchedEffect(state.detailsEntryId) {
        state.detailsEntryId?.let { entryId ->
            screenModel.consumeDetailsEntry()
            navigator.push(EntryScreen(entryId, fromSource = true))
        }
    }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(MR.strings.sourceMigrationReview_chooseReplacement),
                style = MaterialTheme.typography.header,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.padding.medium)
                    .padding(horizontal = MaterialTheme.padding.medium),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.padding.extraSmall))
            when {
                !state.isLoaded -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.padding.large),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.candidates.isNotEmpty() -> {
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        itemsIndexed(
                            items = state.candidates,
                            key = { _, candidate ->
                                "${candidate.candidate.targetSourceId}:${candidate.candidate.targetUrl}"
                            },
                        ) { index, candidate ->
                            SourceMigrationCandidateRow(
                                item = candidate,
                                selected = candidate.candidate.targetSourceId == item.targetSourceId &&
                                    candidate.candidate.targetUrl == item.targetUrl,
                                enabled = !state.isWorking,
                                onClick = { screenModel.select(candidate.candidate) },
                                onDetailsClick = { screenModel.openDetails(candidate.candidate) },
                            )
                            if (index < state.candidates.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
            if (state.failed) {
                Text(
                    text = stringResource(MR.strings.sourceMigrationReview_replacementRejected),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider()
            if (item.selectedTargetEntryId != null) {
                TextButton(
                    onClick = screenModel::clearSelection,
                    enabled = !state.isWorking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.padding.medium),
                ) {
                    Text(text = stringResource(MR.strings.sourceMigrationReview_clearReplacement))
                }
            }
            Button(
                onClick = screenModel::searchAgain,
                enabled = !state.isWorking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.padding.medium,
                        vertical = MaterialTheme.padding.small,
                    ),
            ) {
                if (state.isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                }
                Text(
                    text = stringResource(MR.strings.sourceMigrationReview_searchAgain),
                    modifier = Modifier.padding(start = MaterialTheme.padding.small),
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
        supportingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.sourceName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                BadgeGroup {
                    Badge(
                        text = stringResource(
                            when (item.candidate.matchKind) {
                                SourceMigrationMatchKind.EXACT -> MR.strings.sourceMigrationReview_exactMatch
                                SourceMigrationMatchKind.SIMILAR -> MR.strings.sourceMigrationReview_similarMatch
                                SourceMigrationMatchKind.MANUAL -> MR.strings.sourceMigrationReview_manualMatch
                            },
                        ),
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(MR.strings.selected),
                        tint = MaterialTheme.colorScheme.primary,
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
