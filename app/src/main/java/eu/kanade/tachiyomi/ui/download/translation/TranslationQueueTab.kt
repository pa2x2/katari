package eu.kanade.tachiyomi.ui.download.translation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.entry.translation.EntryTranslateSetupSheet
import eu.kanade.presentation.entry.translation.entryTranslateStatusText
import mihon.entry.interactions.translate.EntryTranslateFailure
import mihon.entry.interactions.translate.EntryTranslateStatus
import mihon.entry.interactions.translate.EntryTranslateWaiting
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

/** The translation queue as a tab of the queue screen, with its own Pause and Cancel all. */
@Composable
internal fun translationQueueTab(model: TranslationQueueScreenModel): TabContent {
    val rows by model.rows.collectAsStateWithLifecycle()
    val waiting by model.waiting.collectAsStateWithLifecycle()
    val paused by model.paused.collectAsStateWithLifecycle()
    val setup by model.translation.sheet.collectAsStateWithLifecycle()
    val queue = rows.orEmpty()
    return TabContent(
        titleRes = MR.strings.label_translations_tab,
        badgeNumber = queue.size.takeIf { it > 0 },
        actions = if (queue.isEmpty()) {
            emptyList()
        } else {
            listOf(
                AppBar.OverflowAction(
                    title = stringResource(MR.strings.action_cancel_all),
                    onClick = model::cancelAll,
                ),
            )
        },
        content = { contentPadding, _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (rows != null && queue.isEmpty()) {
                    EmptyScreen(
                        stringRes = MR.strings.information_no_translations,
                        modifier = Modifier.padding(contentPadding),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 88.dp),
                    ) {
                        items(queue, key = { it.chapter.id }) { row ->
                            QueueRow(
                                row = row,
                                held = when {
                                    paused -> stringResource(MR.strings.chapter_translation_paused)
                                    else -> waiting?.let { heldText(it) }
                                },
                                onStartNow = { model.startNow(row) },
                                onRetry = { model.retry(row) },
                                onCancel = { model.cancel(row) },
                            )
                        }
                    }
                }
                if (queue.isNotEmpty()) {
                    SmallExtendedFloatingActionButton(
                        text = {
                            Text(stringResource(if (paused) MR.strings.action_resume else MR.strings.action_pause))
                        },
                        icon = {
                            Icon(
                                imageVector = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = null,
                            )
                        },
                        onClick = { if (paused) model.resume() else model.pause() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(contentPadding)
                            .padding(16.dp),
                    )
                }
            }
            setup?.let { EntryTranslateSetupSheet(it, model.translation) }
        },
    )
}

/**
 * One queued chapter. A queued chapter shows [held] instead of its status while the queue is paused or waits for a
 * charger or Wi-Fi.
 */
@Composable
private fun QueueRow(
    row: TranslationQueueRow,
    held: String?,
    onStartNow: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val status = row.status
    Column {
        ListItem(
            supportingContent = {
                val statusText = held.takeIf { status == EntryTranslateStatus.Queued }
                    ?: entryTranslateStatusText(status)
                Text(
                    text = "${row.chapter.name} · $statusText",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (status is EntryTranslateStatus.Failed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            },
            trailingContent = {
                RowActions(status = status, onStartNow = onStartNow, onRetry = onRetry, onCancel = onCancel)
            },
            content = { Text(row.entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        if (status is EntryTranslateStatus.Translating) {
            val progress = status.progress
            if (progress.total > 0) {
                LinearProgressIndicator(
                    progress = { progress.done.toFloat() / progress.total },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun RowActions(
    status: EntryTranslateStatus,
    onStartNow: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (status is EntryTranslateStatus.Failed) {
            val setupRequired = status.failure == EntryTranslateFailure.SetupRequired
            TextButton(onClick = onRetry) {
                Text(stringResource(if (setupRequired) MR.strings.action_fix else MR.strings.action_retry))
            }
        }
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(MR.strings.action_menu_overflow_description),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (status == EntryTranslateStatus.Queued) {
                    DropdownMenuItem(
                        text = { Text(stringResource(MR.strings.action_start_now)) },
                        onClick = {
                            expanded = false
                            onStartNow()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.action_cancel_translation)) },
                    onClick = {
                        expanded = false
                        onCancel()
                    },
                )
            }
        }
    }
}

@Composable
private fun heldText(waiting: EntryTranslateWaiting): String = when (waiting) {
    EntryTranslateWaiting.Charger -> stringResource(MR.strings.translate_waiting_for_charger)
    EntryTranslateWaiting.Wifi -> stringResource(MR.strings.translate_waiting_for_wifi)
}
