package eu.kanade.presentation.library.update

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.data.library.LibraryUpdateStart
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.feature.library.update.report.LibraryUpdateRunSummary
import mihon.feature.library.update.report.subscribeLatestSummary
import mihon.presentation.kit.feedback.SnackbarDuration
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.feedback.SnackbarResult
import tachiyomi.i18n.*
import tachiyomi.i18n.context.pluralStringResource
import tachiyomi.i18n.context.stringResource
import kotlin.time.Clock

/**
 * Starts library updates from a screen, saying in a snackbar when the update has to wait for another profile's or one
 * is already running. Screens without their own summary of the latest update use
 * [rememberReportingLibraryUpdateStarter], which also sums a started update up once it finishes and offers to check
 * what the rules skipped.
 */
class LibraryUpdateStarter internal constructor(
    private val onStarted: (requestedAt: Long) -> Unit,
    private val onQueued: (aheadProfileName: String) -> Unit,
    private val onRefused: () -> Unit,
) {
    /** @return whether the update was started or queued. */
    suspend fun start(start: suspend () -> LibraryUpdateStart): Boolean {
        val requestedAt = Clock.System.now().toEpochMilliseconds()
        return when (val result = start()) {
            LibraryUpdateStart.Started -> {
                onStarted(requestedAt)
                true
            }
            is LibraryUpdateStart.Queued -> {
                onStarted(requestedAt)
                onQueued(result.aheadProfileName)
                true
            }
            LibraryUpdateStart.AlreadyQueued -> {
                onRefused()
                false
            }
        }
    }
}

@Composable
fun rememberLibraryUpdateStarter(snackbarHostState: SnackbarHostState): LibraryUpdateStarter =
    rememberStarter(snackbarHostState, onStarted = {})

/** @param startSkippedOfLatest starts an update of the entries the latest one skipped. */
@Composable
fun rememberReportingLibraryUpdateStarter(
    snackbarHostState: SnackbarHostState,
    startSkippedOfLatest: suspend () -> LibraryUpdateStart,
): LibraryUpdateStarter {
    val scope = rememberCoroutineScope()
    // Only an update that began after the request counts, so an earlier run's result is never reported as this one's.
    var awaitedRequestedAt by rememberSaveable { mutableStateOf<Long?>(null) }
    val starter = rememberStarter(snackbarHostState, onStarted = { awaitedRequestedAt = it })
    val currentStartSkippedOfLatest by rememberUpdatedState(startSkippedOfLatest)
    LibraryUpdateResultSnackbar(
        requestedAt = awaitedRequestedAt,
        snackbarHostState = snackbarHostState,
        onShown = { awaitedRequestedAt = null },
        onCheckSkipped = { scope.launch { starter.start(currentStartSkippedOfLatest) } },
    )
    return starter
}

@Composable
private fun rememberStarter(
    snackbarHostState: SnackbarHostState,
    onStarted: (requestedAt: Long) -> Unit,
): LibraryUpdateStarter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnStarted by rememberUpdatedState(onStarted)
    return remember(snackbarHostState) {
        LibraryUpdateStarter(
            onStarted = { currentOnStarted(it) },
            onQueued = { aheadProfileName ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.stringResource(MR.strings.library_update_queued, aheadProfileName),
                    )
                }
            },
            onRefused = {
                scope.launch {
                    snackbarHostState.showSnackbar(context.stringResource(MR.strings.update_already_running))
                }
            },
        )
    }
}

@Composable
private fun LibraryUpdateResultSnackbar(
    requestedAt: Long?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
    onCheckSkipped: () -> Unit,
) {
    val context = LocalContext.current
    val reportRepository = remember { context.appGraph.libraryUpdateReportRepository }
    val currentOnShown by rememberUpdatedState(onShown)
    val currentOnCheckSkipped by rememberUpdatedState(onCheckSkipped)
    LaunchedEffect(requestedAt) {
        if (requestedAt == null) return@LaunchedEffect
        val summary = reportRepository.subscribeLatestSummary()
            .filterNotNull()
            .first { it.run.startedAt >= requestedAt && !it.isRunning }
        val result = snackbarHostState.showSnackbar(
            message = context.libraryUpdateResultMessage(summary),
            actionLabel = context.stringResource(MR.strings.library_update_check_skipped).takeIf {
                summary.skipped > 0
            },
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        // Clearing the request restarts this effect, so it has to wait until the snackbar is done.
        currentOnShown()
        if (result == SnackbarResult.ActionPerformed) currentOnCheckSkipped()
    }
}

private fun Context.libraryUpdateResultMessage(summary: LibraryUpdateRunSummary): String {
    val nothingChecked = summary.checked == 0
    val headline = stringResource(
        if (nothingChecked) MR.strings.library_update_nothing_checked else MR.strings.library_update_done,
    )
    // Switched-off entries are left out by the user's own choice, so they are only worth naming when they are why a
    // refresh, such as one of a switched-off category's page, checked nothing.
    val switchedOff = summary.switchedOff.takeIf { nothingChecked && it > 0 }?.let {
        pluralStringResource(MR.plurals.library_update_switched_off, it, it)
    }
    return (listOf(headline) + libraryUpdateOutcomeCounts(summary) + listOfNotNull(switchedOff)).joinToString(" · ")
}
