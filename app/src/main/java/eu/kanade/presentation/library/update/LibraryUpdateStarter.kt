package eu.kanade.presentation.library.update

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mihon.feature.library.update.report.LibraryUpdateRunSummary
import mihon.feature.library.update.report.subscribeLatestSummary
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.i18n.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Clock

/**
 * Starts library updates from a screen. A started update sums itself up in a snackbar once it finishes, offering to
 * check what the rules skipped; a refused start says an update is already running.
 */
class LibraryUpdateStarter internal constructor(
    private val onStarted: (requestedAt: Long) -> Unit,
    private val onRefused: () -> Unit,
) {
    /** @param start starts the update, returning false when one is already running. */
    suspend fun start(start: suspend () -> Boolean): Boolean {
        val requestedAt = Clock.System.now().toEpochMilliseconds()
        val started = start()
        if (started) onStarted(requestedAt) else onRefused()
        return started
    }
}

/** @param startSkippedOfLatest starts an update of the entries the latest one skipped. */
@Composable
fun rememberLibraryUpdateStarter(
    snackbarHostState: SnackbarHostState,
    startSkippedOfLatest: suspend () -> Boolean,
): LibraryUpdateStarter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Only an update that began after the request counts, so an earlier run's result is never reported as this one's.
    var awaitedRequestedAt by rememberSaveable { mutableStateOf<Long?>(null) }
    val starter = remember(snackbarHostState) {
        LibraryUpdateStarter(
            onStarted = { awaitedRequestedAt = it },
            onRefused = {
                scope.launch {
                    snackbarHostState.showSnackbar(context.stringResource(MR.strings.update_already_running))
                }
            },
        )
    }
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
private fun LibraryUpdateResultSnackbar(
    requestedAt: Long?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
    onCheckSkipped: () -> Unit,
) {
    val context = LocalContext.current
    val reportRepository = remember { Injekt.get<LibraryUpdateReportRepository>() }
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
