package mihon.entry.interactions

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mihon.entry.interactions.download.notification.AndroidEntryDownloadNotifier
import mihon.entry.interactions.download.notification.EntryDownloadErrorNotification
import mihon.entry.interactions.download.notification.EntryDownloadNotificationPresenter
import mihon.entry.interactions.download.notification.EntryDownloadProgressNotification
import mihon.entry.interactions.download.notification.entryDownloadForegroundNotification
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.entry.interactor.GetMergedEntry

internal class EntryDownloadNotificationManager(
    private val context: Context,
    private val downloads: EntryDownloadInteraction,
    private val actions: EntryDownloadNotificationActions,
    private val getMergedEntry: GetMergedEntry,
    private val presenter: EntryDownloadNotificationPresenter = AndroidEntryDownloadNotifier(context),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : EntryDownloadForegroundNotificationProvider {
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            downloads.queueProgressUpdates().collect {
                if (it.state == EntryDownloadState.DOWNLOADING) showProgress(it)
            }
        }
        scope.launch {
            downloads.queueStatusUpdates().collect {
                if (it.state == EntryDownloadState.DOWNLOADING) showProgress(it)
            }
        }
        scope.launch {
            downloads.events().collect { handleEvent(it) }
        }
        scope.launch {
            combine(downloads.queueState, downloads.isRunning, downloads.isPaused, ::NotificationState)
                .distinctUntilChanged()
                .collect(::renderState)
        }
    }

    override val notificationId: Int = EntryDownloadNotifications.ID_PROGRESS

    override fun notification() = context.entryDownloadForegroundNotification()

    private suspend fun renderState(state: NotificationState) {
        val items = state.queue.flatMap(EntryDownloadQueueGroup::items)
        when {
            items.isEmpty() -> presenter.onComplete()
            state.isPaused -> presenter.showPaused()
            state.isRunning -> items.firstOrNull { it.state == EntryDownloadState.DOWNLOADING }?.let {
                showProgress(it)
            }
            else -> presenter.onComplete()
        }
    }

    private suspend fun showProgress(item: EntryDownloadQueueItem) {
        val progressText = item.progressText.takeIf(String::isNotBlank)
        val subtitle = listOfNotNull(item.subtitle.takeIf(String::isNotBlank), progressText)
            .joinToString(" • ")
            .ifBlank { null }
        presenter.showProgress(
            EntryDownloadProgressNotification(
                entryType = item.entryType,
                title = item.title,
                text = subtitle,
                hiddenTitle = progressText,
                maximum = item.progressMax.coerceAtLeast(1),
                current = item.progress,
                indeterminate = item.progress <= 0,
                entryId = getMergedEntry.awaitVisibleTargetId(item.entryId),
            ),
        )
    }

    private suspend fun handleEvent(event: EntryDownloadEvent) {
        when (event) {
            is EntryDownloadEvent.Error -> {
                presenter.showError(
                    EntryDownloadErrorNotification(
                        entryType = event.entryType,
                        title = listOfNotNull(event.title, event.subtitle)
                            .joinToString(": ")
                            .ifBlank {
                                context.stringResource(tachiyomi.i18n.MR.strings.download_notifier_downloader_title)
                            },
                        message = event.message.resolve(context),
                        entryId = event.entryId?.let { getMergedEntry.awaitVisibleTargetId(it) },
                    ),
                )
            }
            is EntryDownloadEvent.Warning -> {
                presenter.showWarning(
                    reason = event.message.resolve(context),
                    timeout = event.timeoutMillis,
                    contentIntent = event.helpUrl?.let { actions.openUrl(context, it) },
                )
            }
        }
    }

    private data class NotificationState(
        val queue: List<EntryDownloadQueueGroup>,
        val isRunning: Boolean,
        val isPaused: Boolean,
    )
}

private fun EntryDownloadMessage.resolve(context: Context): String {
    return when (this) {
        is EntryDownloadMessage.Text -> value
        is EntryDownloadMessage.Resource -> context.stringResource(resource, *args.toTypedArray())
    }
}
