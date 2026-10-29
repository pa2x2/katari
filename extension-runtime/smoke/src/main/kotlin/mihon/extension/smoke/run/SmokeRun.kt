package mihon.extension.smoke.run

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mihon.desktop.extension.ExtensionHostGraph
import mihon.extension.smoke.classification.CloudflareProbe
import mihon.extension.smoke.classification.FailureClassification
import mihon.extension.smoke.options.SmokeOptions
import mihon.extension.smoke.results.ExtensionResult
import mihon.extension.smoke.results.HeapSummary
import mihon.extension.smoke.results.Outcome
import mihon.extension.smoke.results.OutcomeKind
import mihon.extension.smoke.results.RunSettings
import mihon.extension.smoke.results.SmokeResults
import mihon.extension.smoke.results.StoreResult
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.TimeSource

/**
 * One run over the stores of the list: each store's listings are read, the selected ones installed through the host a
 * few at a time, and each loaded extension's sources called. Extensions stay installed until the run ends, so the heap
 * after each install includes every extension before it, as in an app with all of them installed.
 */
internal class SmokeRun(private val options: SmokeOptions, private val host: ExtensionHostGraph) {

    private val classification = FailureClassification(CloudflareProbe(host.networkHelper.client, options.callTimeout))
    private val installation = ExtensionInstallation(host.extensionManager, options.installTimeout)
    private val exercise = SourceExercise(classification, options.callTimeout)

    suspend fun run(storeUrls: List<String>): SmokeResults {
        val startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        val start = TimeSource.Monotonic.markNow()
        val baselineHeap = HeapMeter.liveHeapMb()

        val urls = storeUrls.filter { url -> options.stores.isEmpty() || options.stores.any { it in url } }
        val listed = readStores(host, urls)
        val selection = ListingSelection(options, host)
        val selections = listed.associateWith(selection::select)
        val toInstall = selections.flatMap { (store, selected) -> selected.toInstall.map { store.url to it } }
        listed.forEach { store -> println(store.summary(selections.getValue(store))) }

        val done = AtomicInteger()
        val permits = Semaphore(options.parallelism)
        val installed = coroutineScope {
            toInstall.map { (store, listing) ->
                async {
                    permits.withPermit { installAndCall(store, listing) }.also { result ->
                        println(
                            "[${done.incrementAndGet()}/${toInstall.size}] ${result.name}: ${result.outcome.summary}",
                        )
                    }
                }
            }.awaitAll()
        }

        val extensions = selections.values.flatMap { it.counted } + installed
        return SmokeResults(
            startedAt = startedAt.toString(),
            durationSeconds = start.elapsedNow().inWholeSeconds,
            settings = settings(),
            stores = listed.map { store ->
                StoreResult(
                    url = store.url,
                    name = store.store?.name,
                    error = store.error,
                    listed = store.listings.size,
                    leftOut = selections.getValue(store).leftOut,
                )
            },
            extensions = extensions,
            heap = HeapSummary(
                baselineMb = baselineHeap,
                peakAfterInstallMb = installed.mapNotNull { it.install?.heapAfterMb }.maxOrNull() ?: baselineHeap,
                finalMb = HeapMeter.liveHeapMb(),
                finalMetaspaceMb = HeapMeter.metaspaceMb(),
            ),
        )
    }

    private suspend fun installAndCall(store: String, listing: Extension.Available): ExtensionResult {
        val family = host.extensionKinds.familyOf(listing)
        val attempt = installation.install(listing)
        val measurements = attempt.measurements.copy(heapAfterMb = HeapMeter.liveHeapMb())
        val result = when (attempt) {
            is InstallAttempt.Failed -> listing.toResult(store, family, classification.ofInstall(attempt.error))
            is InstallAttempt.TimedOut -> listing.toResult(
                store,
                family,
                Outcome(
                    OutcomeKind.TIMEOUT,
                    "Install took over ${options.installTimeout}, last at ${attempt.lastStep}",
                ),
            )
            is InstallAttempt.Installed -> when (val extension = attempt.extension) {
                is Extension.NotLoaded -> listing.toResult(store, family, classification.ofNotLoaded(extension.reason))
                is Extension.Loaded -> {
                    val sources = callOrder(extension.sources).map { source ->
                        exercise.exercise(source, siteUrl(listing, source))
                    }
                    val outcome = when {
                        sources.isEmpty() -> Outcome(OutcomeKind.LOAD_FAILED, "Loaded without sources")
                        else -> sources.firstOrNull { !it.outcome.isWorking }?.outcome ?: Outcome.Working
                    }
                    listing.toResult(store, family, outcome, sources = sources)
                }
            }
        }
        return result.copy(install = measurements)
    }

    /** English sources first, then the ones for every language, as far as the run calls an extension's sources. */
    private fun callOrder(sources: List<UnifiedSource>): List<UnifiedSource> {
        val ordered = sources.sortedBy { source ->
            when ((source as? EntryCatalogueSource)?.lang) {
                "en" -> 0
                "all", "", null -> 1
                else -> 2
            }
        }
        return options.maxSourcesPerExtension?.let(ordered::take) ?: ordered
    }

    /** The site the listing gives for [source], for the report and the Cloudflare probe. */
    private fun siteUrl(listing: Extension.Available, source: UnifiedSource): String? =
        (listing.sources.firstOrNull { it.id == source.id } ?: listing.sources.firstOrNull())
            ?.baseUrl
            ?.takeIf { it.isNotBlank() }

    private fun settings() = RunSettings(
        storeList = options.storeList.toString(),
        families = options.families.map { it.name },
        stores = options.stores,
        extensions = options.extensions,
        maxPerStore = options.maxPerStore.asMap(),
        maxSourcesPerExtension = options.maxSourcesPerExtension,
        parallelism = options.parallelism,
        callTimeoutSeconds = options.callTimeout.inWholeSeconds,
        installTimeoutSeconds = options.installTimeout.inWholeSeconds,
        maxHeapMb = HeapMeter.maxHeapMb(),
    )
}

private fun ListedStore.summary(selection: StoreSelection): String {
    val install = selection.toInstall.size
    val counts = "${listings.size} listed, $install to install, ${selection.counted.size} counted only"
    return "${store?.name ?: url}: $counts${error?.let { ", unreadable: $it" }.orEmpty()}"
}

private val Outcome.summary: String
    get() = listOfNotNull(kind.label, member ?: errorType, detail.takeIf { member == null }?.take(160))
        .filter { it.isNotBlank() }
        .joinToString(" - ")
