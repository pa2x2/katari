package app.katari.tools.extensioncompatibility.results

import kotlinx.serialization.Serializable

/**
 * Everything one run found, as its JSON report holds it. The next run in the same report folder reads it back to say
 * what changed, so fields are only added, with defaults.
 */
@Serializable
data class SmokeResults(
    /** Local date and time the run started, ISO-8601. */
    val startedAt: String,
    val durationSeconds: Long,
    val settings: RunSettings,
    val stores: List<StoreResult>,
    val extensions: List<ExtensionResult>,
    val heap: HeapSummary,
)

/** What the run was asked to cover, as given on the command line. */
@Serializable
data class RunSettings(
    val storeList: String,
    val families: List<String>,
    val stores: List<String>,
    val extensions: List<String>,
    /** Per store text, or `*` for every store; absent when no store was limited. */
    val maxPerStore: Map<String, Int>,
    val maxSourcesPerExtension: Int?,
    val parallelism: Int,
    val callTimeoutSeconds: Long,
    val installTimeoutSeconds: Long,
    val maxHeapMb: Long,
)

@Serializable
data class StoreResult(
    /** As the store list gives it; extensions are matched to their store through it. */
    val url: String,
    val name: String? = null,
    /** Why the store couldn't be read; it then lists nothing. */
    val error: String? = null,
    val listed: Int = 0,
    /** Listed extensions the run's filters and limits left out. */
    val leftOut: Int = 0,
)

@Serializable
data class ExtensionResult(
    val store: String,
    val pkgName: String,
    val name: String,
    val versionName: String,
    val libVersion: String,
    /** An [eu.kanade.tachiyomi.extension.kind.ExtensionFamily] name, or `UNKNOWN` for a library no family has. */
    val family: String,
    val outcome: Outcome,
    /** Absent for a listing that wasn't installed. */
    val install: InstallMeasurements? = null,
    val sources: List<SourceResult> = emptyList(),
) {
    val key: String get() = "$store $pkgName"
}

@Serializable
data class InstallMeasurements(
    /** Downloading the file and checking its signature, until it's handed to the platform's installer. */
    val downloadMs: Long? = null,
    /** Converting an apk's dex code to a jar; absent for a file extension, which isn't converted. */
    val conversionMs: Long? = null,
    /** From the installed step until the extension manager lists the extension, loaded or not. */
    val loadMs: Long? = null,
    /** Live heap right after the extension loaded, every extension loaded before it still loaded. */
    val heapAfterMb: Long? = null,
)

@Serializable
data class SourceResult(
    val id: Long,
    val name: String,
    val lang: String,
    val outcome: Outcome,
    val calls: List<CallResult>,
)

@Serializable
data class CallResult(
    val call: SourceCall,
    val outcome: Outcome,
    val durationMs: Long,
    /** Entries on the page, or chapters in the list; absent for a call that failed and for details. */
    val items: Int? = null,
)

@Serializable
enum class SourceCall(val label: String) {
    POPULAR("popular page 1"),
    LATEST("latest page 1"),
    DETAILS("details of the first entry"),
    CHAPTERS("chapters of the first entry"),
}

@Serializable
data class HeapSummary(
    val baselineMb: Long,
    /** The highest of the extensions' [InstallMeasurements.heapAfterMb]. */
    val peakAfterInstallMb: Long,
    /** Live heap after the last call, with every extension of the run still loaded. */
    val finalMb: Long,
    val finalMetaspaceMb: Long,
)
