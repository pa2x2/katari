package app.katari.tools.extensioncompatibility.run

import app.katari.tools.extensioncompatibility.classification.FailureClassification
import app.katari.tools.extensioncompatibility.results.CallResult
import app.katari.tools.extensioncompatibility.results.Outcome
import app.katari.tools.extensioncompatibility.results.OutcomeKind
import app.katari.tools.extensioncompatibility.results.SourceCall
import app.katari.tools.extensioncompatibility.results.SourceResult
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * What browsing a source first asks of it: popular page 1, latest page 1 where it has one, then the details and the
 * chapter list of popular's first entry. The source's outcome is its first call that didn't work.
 */
internal class SourceExercise(
    private val classification: FailureClassification,
    private val callTimeout: Duration,
) {

    suspend fun exercise(source: UnifiedSource, siteUrl: String?): SourceResult {
        val catalogue = source as? EntryCatalogueSource
        val calls = mutableListOf<CallResult>()
        val popular = call(source, SourceCall.POPULAR, siteUrl, calls, { it.items.size }) {
            source.getPopularContent(1)
        }
        if (catalogue?.supportsLatest != false) {
            call(source, SourceCall.LATEST, siteUrl, calls, { it.items.size }) { source.getLatestUpdates(1) }
        }
        val first = popular?.items?.firstOrNull()
        if (first != null) {
            call(source, SourceCall.DETAILS, siteUrl, calls, { null }) {
                source.refresh(first, emptyList(), fetchDetails = true, fetchChapters = false)
            }
            call(source, SourceCall.CHAPTERS, siteUrl, calls, { it.chapters.size }) {
                source.refresh(first, emptyList(), fetchDetails = false, fetchChapters = true)
            }
        }
        val outcome = calls.firstOrNull { !it.outcome.isWorking }?.outcome
            ?: if (popular != null && first == null) classification.ofEmptyPage(siteUrl) else Outcome.Working
        return SourceResult(
            id = source.id,
            name = source.name,
            lang = catalogue?.lang.orEmpty(),
            outcome = outcome,
            calls = calls,
        )
    }

    private suspend fun <T> call(
        source: UnifiedSource,
        call: SourceCall,
        siteUrl: String?,
        calls: MutableList<CallResult>,
        items: (T) -> Int?,
        block: suspend () -> T,
    ): T? {
        val start = TimeSource.Monotonic.markNow()
        val result = withAbandoningTimeout(callTimeout, block)
        val took = start.elapsedNow().inWholeMilliseconds
        if (result == null) {
            calls += CallResult(call, Outcome(OutcomeKind.TIMEOUT, "${call.label} took over $callTimeout"), took)
            return null
        }
        return result.fold(
            onSuccess = { value ->
                calls += CallResult(call, Outcome.Working, took, items(value))
                value
            },
            onFailure = { error ->
                // The report keeps the error's text; the run's log keeps where it was thrown
                logcat(LogPriority.WARN) { "${source.name} (${source.id}): ${call.label} failed\n${error.asLog()}" }
                calls += CallResult(call, classification.ofCall(error, siteUrl), took)
                null
            },
        )
    }
}
