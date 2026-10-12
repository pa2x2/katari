package tachiyomi.domain.source.service

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Waits for the installed sources to be registered. Extensions load in the background after the process starts, so
 * until then a lookup can't tell a missing source from one that hasn't loaded yet. Work that would act on a missing
 * source (fail it, drop it, report it) waits here first.
 *
 * Bounded so that sources that never finish loading can't hold that work back for good; it then proceeds with what
 * has loaded.
 */
suspend fun awaitSourcesInitialized(isInitialized: StateFlow<Boolean>) {
    withTimeoutOrNull(30.seconds) {
        isInitialized.first { it }
    }
}

suspend fun SourceManager.awaitInitialized() = awaitSourcesInitialized(isInitialized)
