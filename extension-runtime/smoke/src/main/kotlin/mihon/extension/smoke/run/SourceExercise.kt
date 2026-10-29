package mihon.extension.smoke.run

import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.extension.smoke.classification.FailureClassification
import mihon.extension.smoke.results.CallResult
import mihon.extension.smoke.results.Outcome
import mihon.extension.smoke.results.OutcomeKind
import mihon.extension.smoke.results.SourceCall
import mihon.extension.smoke.results.SourceResult
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
        val popular = call(SourceCall.POPULAR, siteUrl, calls, { it.items.size }) { source.getPopularContent(1) }
        if (catalogue?.supportsLatest != false) {
            call(SourceCall.LATEST, siteUrl, calls, { it.items.size }) { source.getLatestUpdates(1) }
        }
        val first = popular?.items?.firstOrNull()
        if (first != null) {
            call(SourceCall.DETAILS, siteUrl, calls, { null }) {
                source.refresh(first, emptyList(), fetchDetails = true, fetchChapters = false)
            }
            call(SourceCall.CHAPTERS, siteUrl, calls, { it.chapters.size }) {
                source.refresh(first, emptyList(), fetchDetails = false, fetchChapters = true)
            }
        }
        val outcome = calls.firstOrNull { !it.outcome.isWorking }?.outcome
            ?: if (popular != null && first == null) {
                Outcome(
                    OutcomeKind.EXTENSION_ERROR,
                    "Popular page 1 has no entries",
                    errorType = "Empty page",
                    url = siteUrl,
                )
            } else {
                Outcome.Working
            }
        return SourceResult(
            id = source.id,
            name = source.name,
            lang = catalogue?.lang.orEmpty(),
            outcome = outcome,
            calls = calls,
        )
    }

    private suspend fun <T> call(
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
                calls += CallResult(call, classification.ofCall(error, siteUrl), took)
                null
            },
        )
    }
}
