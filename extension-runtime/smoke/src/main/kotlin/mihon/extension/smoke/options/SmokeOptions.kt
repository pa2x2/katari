package mihon.extension.smoke.options

import eu.kanade.tachiyomi.extension.kind.ExtensionFamily
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** What one run covers and how, from the command line ([parse]). */
data class SmokeOptions(
    val storeList: Path,
    val reportDirectory: Path,
    /** An earlier report's JSON to say what changed since; null for the latest one in [reportDirectory]. */
    val compareWith: Path?,
    /** Empty for every family. */
    val families: Set<ExtensionFamily>,
    /** Texts a store's URL must contain one of; empty for every store. */
    val stores: List<String>,
    /** Texts an extension's package or name must contain one of; empty for every extension. */
    val extensions: List<String>,
    val maxPerStore: StoreLimits,
    /** Null to call every source of an extension. */
    val maxSourcesPerExtension: Int?,
    val parallelism: Int,
    val callTimeout: Duration,
    val installTimeout: Duration,
    val keepData: Boolean,
) {
    companion object {
        /** Throws [UsageException] for arguments that don't make a run. */
        fun parse(args: Array<String>): SmokeOptions {
            var storeList: Path? = null
            var reports: Path? = null
            var compareWith: Path? = null
            val families = mutableSetOf<ExtensionFamily>()
            val stores = mutableListOf<String>()
            val extensions = mutableListOf<String>()
            var limits = StoreLimits()
            var maxSources: Int? = null
            var parallelism = 4
            var callTimeout = 30.seconds
            var installTimeout = 180.seconds
            var keepData = false

            val remaining = ArrayDeque(args.toList())
            fun value(option: String): String =
                remaining.removeFirstOrNull() ?: throw UsageException("$option needs a value")
            while (remaining.isNotEmpty()) {
                when (val option = remaining.removeFirst()) {
                    "--stores" -> storeList = Path(value(option))
                    "--reports" -> reports = Path(value(option))
                    "--compare" -> compareWith = Path(value(option))
                    "--family" -> families += family(value(option))
                    "--store" -> stores += value(option)
                    "--extension" -> extensions += value(option)
                    "--max-per-store" -> limits = limits.with(value(option))
                    "--max-sources" -> maxSources = positive(option, value(option))
                    "--parallelism" -> parallelism = positive(option, value(option))
                    "--timeout" -> callTimeout = positive(option, value(option)).seconds
                    "--install-timeout" -> installTimeout = positive(option, value(option)).seconds
                    "--keep-data" -> keepData = true
                    "--help", "-h" -> throw UsageException(null)
                    else -> throw UsageException("Unknown option $option")
                }
            }
            return SmokeOptions(
                storeList = storeList ?: throw UsageException("--stores is required"),
                reportDirectory = reports ?: throw UsageException("--reports is required"),
                compareWith = compareWith?.also {
                    if (!it.isRegularFile()) throw UsageException("--compare needs an earlier report's JSON, not $it")
                },
                families = families,
                stores = stores,
                extensions = extensions,
                maxPerStore = limits,
                maxSourcesPerExtension = maxSources,
                parallelism = parallelism,
                callTimeout = callTimeout,
                installTimeout = installTimeout,
                keepData = keepData,
            )
        }

        private fun family(name: String): ExtensionFamily =
            ExtensionFamily.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: throw UsageException("Unknown family $name; one of $FAMILY_NAMES")

        private fun positive(option: String, text: String): Int =
            text.toIntOrNull()?.takeIf { it > 0 } ?: throw UsageException("$option needs a positive number, not $text")

        private val FAMILY_NAMES = ExtensionFamily.entries.joinToString("|") { it.name.lowercase() }

        val USAGE = """
            |Usage: --stores <file> --reports <directory> [options]
            |  --stores <file>            store index URLs, one per line; blank lines and # comments are ignored
            |  --reports <directory>      where the Markdown and JSON reports go; the latest one there is diffed against
            |  --compare <file>           diff against this earlier report's JSON instead, such as the last run with the
            |                             same stores and limits
            |  --family <name>            only this family: $FAMILY_NAMES (repeatable)
            |  --store <text>             only stores whose URL contains the text (repeatable)
            |  --extension <text>         only extensions whose package or name contains the text (repeatable)
            |  --max-per-store [<text>=]<n>  install at most n extensions of each store, or of the stores whose URL
            |                             contains the text, spread evenly over the store's list (repeatable)
            |  --max-sources <n>          call at most n sources of each extension, English first (default: all)
            |  --parallelism <n>          extensions installed and called at once (default: 4)
            |  --timeout <seconds>        for each source call (default: 30)
            |  --install-timeout <seconds>  for downloading, converting and loading one extension (default: 180)
            |  --keep-data                keep the run's data and cache folder, its log among them
        """.trimMargin()
    }
}

/** How many extensions of a store a run installs at most: a default, and per store text. */
data class StoreLimits(
    val default: Int? = null,
    val byStoreText: Map<String, Int> = emptyMap(),
) {
    fun forStore(url: String): Int? = byStoreText.entries.firstOrNull { (text, _) -> text in url }?.value ?: default

    /** As the settings record them: per store text, `*` for the default. */
    fun asMap(): Map<String, Int> = byStoreText + listOfNotNull(default?.let { "*" to it })

    internal fun with(argument: String): StoreLimits {
        val text = argument.substringBeforeLast('=', missingDelimiterValue = "")
        val count = argument.substringAfterLast('=').toIntOrNull()?.takeIf { it > 0 }
            ?: throw UsageException("--max-per-store needs [<text>=]<n>, not $argument")
        return if (text.isEmpty()) copy(default = count) else copy(byStoreText = byStoreText + (text to count))
    }
}

/** Arguments that don't make a run; a null message asks only for the usage. */
class UsageException(message: String?) : Exception(message)
