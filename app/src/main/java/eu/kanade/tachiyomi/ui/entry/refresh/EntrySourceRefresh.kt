package eu.kanade.tachiyomi.ui.entry.refresh

import android.content.Context
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.ui.entry.EntryStateAccess
import eu.kanade.tachiyomi.ui.entry.members.EntryMembers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.source.EntrySourceRefreshFailure
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.interactions.source.EntrySourceRefreshRequest
import mihon.entry.interactions.source.EntrySourceRefreshResult
import mihon.presentation.kit.feedback.SnackbarHostState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/**
 * Fetching the details and chapters of every member an entry screen covers from their sources. One refresh runs at a
 * time, the screen shows it running, and its first failure ends it with a message.
 */
@AssistedInject
class EntrySourceRefresh(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val context: Context,
    @Assisted private val snackbarHostState: SnackbarHostState,
    @Assisted private val state: EntryStateAccess,
    @Assisted private val members: EntryMembers,
    private val sourceRefreshFeature: EntrySourceRefreshFeature,
    private val entryTypePresentationFeature: EntryTypePresentationFeature,
) {

    @AssistedFactory
    fun interface Factory {
        fun create(
            scope: CoroutineScope,
            context: Context,
            snackbarHostState: SnackbarHostState,
            state: EntryStateAccess,
            members: EntryMembers,
        ): EntrySourceRefresh
    }

    private var job: Job? = null

    fun fetchAll(manualFetch: Boolean = true) {
        launch(manualFetch = manualFetch, fetchInfo = true, fetchChapters = true)
    }

    fun launch(manualFetch: Boolean, fetchInfo: Boolean, fetchChapters: Boolean): Job {
        job?.cancel()
        return scope.launchIO {
            state.update { it.copy(isRefreshingData = true) }
            try {
                job = coroutineContext[Job]
                fetch(
                    manualFetch = manualFetch,
                    fetchDetails = fetchInfo,
                    fetchChapters = fetchChapters,
                )
            } finally {
                if (job === coroutineContext[Job]) {
                    job = null
                    state.update { it.copy(isRefreshingData = false) }
                }
            }
        }.also { job = it }
    }

    suspend fun await() {
        val refreshJob = job
        if (refreshJob?.isActive == true) {
            refreshJob.join()
        }
    }

    private suspend fun fetch(
        manualFetch: Boolean,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ) {
        state.loaded ?: return
        try {
            // Refreshing a merged entry can bring in members, which are refreshed in the same pass
            val refreshed = mutableSetOf<Long>()
            while (true) {
                val membersToRefresh = members.toRefreshFromSource(manualFetch, state.loaded?.entry)
                    .filter { refreshed.add(it.id) }
                if (membersToRefresh.isEmpty()) break
                for (memberEntry in membersToRefresh) {
                    when (
                        val result = sourceRefreshFeature.refresh(
                            EntrySourceRefreshRequest(
                                entry = memberEntry,
                                fetchDetails = fetchDetails,
                                fetchChildren = fetchChapters,
                                manual = manualFetch,
                            ),
                        )
                    ) {
                        is EntrySourceRefreshResult.Refreshed -> Unit
                        is EntrySourceRefreshResult.SourceUnavailable -> {
                            showFailure(context.stringResource(MR.strings.loader_not_implemented_error))
                            return
                        }
                        is EntrySourceRefreshResult.Failed -> when (val reason = result.reason) {
                            EntrySourceRefreshFailure.NoChildren -> {
                                val entryType = state.loaded?.entry?.type
                                showFailure(
                                    context.stringResource(
                                        entryType.entryTypePresentation(entryTypePresentationFeature)
                                            .noChildrenFoundLabel,
                                    ),
                                )
                                return
                            }
                            is EntrySourceRefreshFailure.Operation -> {
                                logcat(LogPriority.ERROR, reason.error)
                                showFailure(with(context) { reason.error.formattedMessage })
                                return
                            }
                        }
                    }
                }
            }
        } catch (_: CancellationException) {
            // ignore
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            showFailure(with(context) { e.formattedMessage })
        }
    }

    private fun showFailure(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message = message)
        }
    }
}
