package eu.kanade.tachiyomi.ui.download.translation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.entry.translation.EntryTranslateSetupSheet
import eu.kanade.presentation.entry.translation.entryTranslateStatusText
import mihon.entry.interactions.translate.EntryTranslateFailure
import mihon.entry.interactions.translate.EntryTranslateStatus
import mihon.entry.interactions.translate.EntryTranslateWaiting
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.icons.materialsymbols.roundedfilled.Pause
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.FloatingActionButton
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The translation queue as a tab of the queue screen, with its own Pause and Cancel all. */
@Composable
internal fun translationQueueTab(model: TranslationQueueViewModel): TabContent {
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
                        // Room for the Pause button over the end of the list.
                        contentPadding = PaddingValues(
                            bottom = contentPadding.calculateBottomPadding() +
                                KatariTheme.sizes.touchTarget +
                                KatariTheme.spacing.xxl,
                        ),
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
                    FloatingActionButton(
                        label = stringResource(if (paused) MR.strings.action_resume else MR.strings.action_pause),
                        icon = if (paused) {
                            MaterialSymbols.RoundedFilled.PlayArrow
                        } else {
                            MaterialSymbols.RoundedFilled.Pause
                        },
                        onClick = { if (paused) model.resume() else model.pause() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(contentPadding)
                            .padding(KatariTheme.spacing.l),
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
    val statusText = held.takeIf { status == EntryTranslateStatus.Queued } ?: entryTranslateStatusText(status)
    SettingsRow(
        title = row.entry.title,
        trailing = {
            RowActions(status = status, onStartNow = onStartNow, onRetry = onRetry, onCancel = onCancel)
        },
        below = {
            Text(
                text = "${row.chapter.name} · $statusText",
                role = TextRole.Supporting,
                tone = if (status is EntryTranslateStatus.Failed) ContentTone.Error else ContentTone.Inherit,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (status is EntryTranslateStatus.Translating) {
                val progress = status.progress
                LinearProgressIndicator(
                    modifier = Modifier.padding(top = KatariTheme.spacing.s),
                    progress = if (progress.total > 0) {
                        { progress.done.toFloat() / progress.total }
                    } else {
                        null
                    },
                )
            }
        },
    )
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
            Button(
                label = stringResource(if (setupRequired) MR.strings.action_fix else MR.strings.action_retry),
                onClick = onRetry,
                style = ButtonStyle.Text,
            )
        }
        Box {
            IconButton(
                icon = MaterialSymbols.Rounded.MoreVert,
                contentDescription = stringResource(MR.strings.action_menu_overflow_description),
                onClick = { expanded = true },
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (status == EntryTranslateStatus.Queued) {
                    DropdownMenuItem(
                        label = stringResource(MR.strings.action_start_now),
                        onClick = {
                            expanded = false
                            onStartNow()
                        },
                    )
                }
                DropdownMenuItem(
                    label = stringResource(MR.strings.action_cancel_translation),
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
