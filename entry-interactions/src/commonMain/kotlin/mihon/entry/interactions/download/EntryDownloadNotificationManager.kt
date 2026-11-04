package mihon.entry.interactions.download

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mihon.core.platform.PlatformContext
import mihon.entry.interactions.merge.EntryMergeDownloadOwnershipProjection
import mihon.entry.interactions.merge.EntryMergeSubject
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource

/** Turns the download queue's state and events into what [presenter] shows, from [start] on. */
internal class EntryDownloadNotificationManager(
    private val context: PlatformContext,
    private val downloads: EntryDownloadRuntimeCoordinator,
    private val ownership: EntryMergeDownloadOwnershipProjection,
    private val presenter: EntryDownloadNotificationPresenter,
    private val scope: CoroutineScope,
    private val messageResolver: (EntryDownloadMessage) -> String = { it.resolve(context) },
) {
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            downloads.events().collect { handleEvent(it) }
        }
        scope.launch {
            downloads.state
                .distinctUntilChanged()
                .collect(::renderState)
        }
    }

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
        val destination = ownership.resolveDownloadOwners(
            EntryMergeSubject(
                item.profileId,
                item.entryId,
            ),
        )
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
                                context.stringResource(MR.strings.download_notifier_downloader_title)
                            },
                        message = messageResolver(event.message),
                    ),
                )
            }
            is EntryDownloadEvent.Warning -> {
                presenter.showWarning(
                    reason = messageResolver(event.message),
                    timeoutMillis = event.timeoutMillis,
                    helpUrl = event.helpUrl,
                )
            }
        }
    }
}
