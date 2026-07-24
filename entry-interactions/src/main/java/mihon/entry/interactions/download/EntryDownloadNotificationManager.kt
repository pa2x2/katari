package mihon.entry.interactions

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mihon.entry.interactions.download.notification.AndroidEntryDownloadNotifier
import mihon.entry.interactions.download.notification.EntryDownloadErrorNotification
import mihon.entry.interactions.download.notification.EntryDownloadNotificationPresenter
import mihon.entry.interactions.download.notification.EntryDownloadProgressNotification
import mihon.entry.interactions.download.notification.entryDownloadForegroundNotification
import tachiyomi.core.common.i18n.stringResource

internal class EntryDownloadNotificationManager(
    private val context: Context,
    private val downloads: EntryDownloadRuntimeCoordinator,
    private val actions: EntryDownloadNotificationActions,
    private val ownership: EntryMergeDownloadOwnershipProjection,
    private val presenter: EntryDownloadNotificationPresenter = AndroidEntryDownloadNotifier(context),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val messageResolver: (EntryDownloadMessage) -> String = { it.resolve(context) },
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
            downloads.state
                .distinctUntilChanged()
                .collect(::renderState)
        }
    }

    override val notificationId: Int = EntryDownloadNotifications.ID_PROGRESS

    override fun notification() = context.entryDownloadForegroundNotification()

    private suspend fun renderState(state: EntryDownloadRuntimeState) {
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
        val destination = ownership.resolveDownloadOwners(EntryMergeSubject(item.profileId, item.entryId))
        val progressText = item.presentation.description()?.let(messageResolver)
        val subtitle = listOfNotNull(item.subtitle.takeIf(String::isNotBlank), progressText)
            .joinToString(" • ")
            .ifBlank { null }
        presenter.showProgress(
            EntryDownloadProgressNotification(
                destination = EntryDownloadEntryIdentity(
                    profileId = destination.profileId,
                    entryType = item.entryType,
                    entryId = destination.visibleEntryId,
                ),
                title = item.title,
                text = subtitle,
                hiddenTitle = progressText,
                maximum = item.progressMax.coerceAtLeast(1),
                current = item.progress,
                indeterminate = item.progress <= 0,
            ),
        )
    }

    private suspend fun handleEvent(event: EntryDownloadEvent) {
        when (event) {
            is EntryDownloadEvent.Error -> {
                val destination = event.entryIdentity?.let {
                    ownership.resolveDownloadOwners(EntryMergeSubject(it.profileId, it.entryId))
                }
                presenter.showError(
                    EntryDownloadErrorNotification(
                        destination = destination?.let {
                            EntryDownloadEntryIdentity(
                                profileId = it.profileId,
                                entryType = event.entryType,
                                entryId = it.visibleEntryId,
                            )
                        },
                        title = listOfNotNull(event.title, event.subtitle)
                            .joinToString(": ")
                            .ifBlank {
                                context.stringResource(tachiyomi.i18n.MR.strings.download_notifier_downloader_title)
                            },
                        message = messageResolver(event.message),
                    ),
                )
            }
            is EntryDownloadEvent.Warning -> {
                presenter.showWarning(
                    reason = messageResolver(event.message),
                    timeout = event.timeoutMillis,
                    contentIntent = event.helpUrl?.let { actions.openUrl(context, it) },
                )
            }
        }
    }
}
