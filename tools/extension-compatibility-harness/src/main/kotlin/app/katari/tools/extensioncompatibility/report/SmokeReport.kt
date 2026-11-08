package app.katari.tools.extensioncompatibility.report

import app.katari.tools.extensioncompatibility.results.ExtensionResult
import app.katari.tools.extensioncompatibility.results.Outcome
import app.katari.tools.extensioncompatibility.results.OutcomeKind
import app.katari.tools.extensioncompatibility.results.RunSettings
import app.katari.tools.extensioncompatibility.results.SmokeResults

/**
 * The run as a Markdown report for people: counts per family and outcome, the platform gaps by missing member, the
 * other failures, the slowest conversions, the heap and what changed since the previous report. The JSON beside it
 * has every call.
 */
internal object SmokeReport {

    fun markdown(results: SmokeResults, diff: ReportDiff?): String = buildString {
        heading(results)
        stores(results)
        outcomesPerFamily(results)
        gaps(results)
        otherFailures(results)
        slowestConversions(results)
        heap(results)
        changes(diff)
    }

    private fun StringBuilder.heading(results: SmokeResults) {
        val settings = results.settings
        appendLine("# Desktop extension compatibility, ${results.startedAt.replace('T', ' ')}")
        appendLine()
        appendLine("- Took ${results.durationSeconds / 60} min ${results.durationSeconds % 60} s.")
        appendLine("- Store list `${settings.storeList}`.")
        appendLine("- Limits: ${limits(settings).ifEmpty { listOf("none") }.joinToString("; ")}.")
        appendLine(
            "- ${settings.parallelism} extensions at a time, ${settings.callTimeoutSeconds} s per call, " +
                "${settings.installTimeoutSeconds} s per install, ${settings.maxHeapMb} MB maximum heap.",
        )
        appendLine()
    }

    private fun limits(settings: RunSettings): List<String> = buildList {
        if (settings.families.isNotEmpty()) add("families ${settings.families.joinToString()}")
        if (settings.stores.isNotEmpty()) add("stores containing ${settings.stores.joinToString { "`$it`" }}")
        if (settings.extensions.isNotEmpty()) add("extensions matching ${settings.extensions.joinToString { "`$it`" }}")
        settings.maxPerStore.forEach { (store, count) ->
            add("at most $count from ${if (store == "*") "each store" else "stores containing `$store`"}")
        }
        settings.maxSourcesPerExtension?.let { add("at most $it sources per extension") }
    }

    private fun StringBuilder.stores(results: SmokeResults) {
        appendLine("## Stores")
        appendLine()
        table(
            listOf("Store", "Listed", "Left out by limits", "Error"),
            results.stores.map { store ->
                listOf(
                    store.name ?: store.url,
                    store.listed.toString(),
                    store.leftOut.toString(),
                    store.error.orEmpty(),
                )
            },
        )
    }

    private fun StringBuilder.outcomesPerFamily(results: SmokeResults) {
        appendLine("## Outcomes per family")
        appendLine()
        appendLine("Extensions, each counted by its first source or call that didn't work:")
        appendLine()
        countTable(results.extensions.map { it.family to it.outcome.kind })
        appendLine("Sources of the installed extensions:")
        appendLine()
        countTable(
            results.extensions.flatMap { extension ->
                extension.sources.map { extension.family to it.outcome.kind }
            },
        )
    }

    private fun StringBuilder.countTable(entries: List<Pair<String, OutcomeKind>>) {
        if (entries.isEmpty()) {
            appendLine("None.")
            appendLine()
            return
        }
        val kinds = OutcomeKind.entries.filter { kind -> entries.any { it.second == kind } }
        val families = entries.map { it.first }.distinct().sorted()
        val rows = (families + TOTAL).map { family ->
            val inFamily = entries.filter { family == TOTAL || it.first == family }
            listOf(family) + kinds.map { kind -> inFamily.count { it.second == kind }.toString() } +
                inFamily.size.toString()
        }
        table(listOf("Family") + kinds.map { it.label } + TOTAL, rows)
    }

    private fun StringBuilder.gaps(results: SmokeResults) {
        appendLine("## Gaps by missing member")
        appendLine()
        val gaps = results.extensions.byMissingMember()
        if (gaps.isEmpty()) {
            appendLine("None.")
            appendLine()
            return
        }
        table(
            listOf("Member", "Outcome", "Extensions", "Affected"),
            gaps.map { gap ->
                listOf("`${gap.member}`", gap.kind.label, gap.extensions.size.toString(), gap.extensions.names())
            },
        )
    }

    private fun StringBuilder.otherFailures(results: SmokeResults) {
        appendLine("## Other failures")
        appendLine()
        val failures = results.extensions.filter { extension ->
            extension.outcome.kind !in COUNTED_ONLY && !extension.outcome.isWorking && extension.outcome.member == null
        }
        if (failures.isEmpty()) {
            appendLine("None.")
            appendLine()
            return
        }
        failures.groupBy { it.outcome.kind }.toSortedMap().forEach { (kind, extensions) ->
            appendLine("### ${kind.label} (${extensions.size})")
            appendLine()
            extensions.groupBy { it.outcome.errorType.orEmpty() }
                .entries
                .sortedByDescending { it.value.size }
                .forEach { (errorType, inType) ->
                    appendLine("- ${errorType.ifEmpty { "No error type" }} (${inType.size}):")
                    inType.sortedBy { it.name }.forEach { extension ->
                        appendLine("  - ${extension.name}: ${extension.outcome.describe()}")
                    }
                }
            appendLine()
        }
    }

    private fun StringBuilder.slowestConversions(results: SmokeResults) {
        appendLine("## Slowest conversions")
        appendLine()
        val converted = results.extensions.filter { it.install?.conversionMs != null }
        if (converted.isEmpty()) {
            appendLine("None converted.")
            appendLine()
            return
        }
        val conversions = converted.mapNotNull { it.install?.conversionMs }.sorted()
        appendLine(
            "${converted.size} apks converted: median ${conversions[conversions.size / 2]} ms, " +
                "total ${conversions.sum() / 1000} s, run ${results.settings.parallelism} at a time.",
        )
        appendLine()
        table(
            listOf("Extension", "Conversion ms", "Load ms", "Download ms", "Heap after, MB"),
            converted.sortedByDescending { it.install?.conversionMs }.take(SLOWEST).map { extension ->
                val install = extension.install
                listOf(
                    extension.name,
                    install?.conversionMs.text(),
                    install?.loadMs.text(),
                    install?.downloadMs.text(),
                    install?.heapAfterMb.text(),
                )
            },
        )
    }

    private fun StringBuilder.heap(results: SmokeResults) {
        val heap = results.heap
        val loaded = results.extensions.count { it.sources.isNotEmpty() }
        appendLine("## Heap")
        appendLine()
        appendLine("Live heap after a full collection, with every extension installed so far still loaded.")
        appendLine()
        appendLine("- Before the first install: ${heap.baselineMb} MB.")
        appendLine("- Highest after an install: ${heap.peakAfterInstallMb} MB.")
        appendLine(
            "- At the end, $loaded extensions loaded: ${heap.finalMb} MB, metaspace ${heap.finalMetaspaceMb} MB.",
        )
        if (loaded > 0) {
            appendLine("- About ${(heap.finalMb - heap.baselineMb) * 1024 / loaded} KB of heap per loaded extension.")
        }
        appendLine()
    }

    private fun StringBuilder.changes(diff: ReportDiff?) {
        appendLine("## Changes since the previous report")
        appendLine()
        if (diff == null) {
            appendLine("No earlier report in this folder.")
            return
        }
        appendLine(
            "Against `${diff.previousReport}`, over the ${diff.compared} extensions both runs covered " +
                "(${diff.onlyNow} only in this run, ${diff.onlyBefore} only in that one).",
        )
        appendLine()
        changeList("Newly working", diff.newlyWorking) { "was ${it.before.outcome.describe()}" }
        changeList("Newly failing", diff.newlyFailing) { it.after.outcome.describe() }
        changeList("Failing differently", diff.failingDifferently) {
            "${it.before.outcome.describe()}, now ${it.after.outcome.describe()}"
        }
        gapList("Gaps appeared", diff.gapsAppeared)
        gapList("Gaps disappeared", diff.gapsDisappeared)
    }

    private fun StringBuilder.changeList(
        title: String,
        changes: List<ReportDiff.Change>,
        describe: (ReportDiff.Change) -> String,
    ) {
        appendLine("### $title (${changes.size})")
        appendLine()
        changes.sortedBy { it.after.name }.forEach { appendLine("- ${it.after.name}: ${describe(it)}") }
        if (changes.isNotEmpty()) appendLine()
    }

    private fun StringBuilder.gapList(title: String, gaps: List<MissingMember>) {
        appendLine("### $title (${gaps.size})")
        appendLine()
        gaps.forEach { appendLine("- `${it.member}`: ${it.extensions.names()}") }
        if (gaps.isNotEmpty()) appendLine()
    }

    private fun StringBuilder.table(header: List<String>, rows: List<List<String>>) {
        appendLine(header.joinToString(" | ", "| ", " |"))
        appendLine(header.joinToString(" | ", "| ", " |") { "---" })
        rows.forEach { row -> appendLine(row.joinToString(" | ", "| ", " |") { it.cell() }) }
        appendLine()
    }

    private fun Outcome.describe(): String =
        listOfNotNull(
            kind.label,
            member?.let { "`$it`" },
            errorType,
            url,
            detail.takeIf { member == null && it.isNotBlank() }?.take(MAX_DETAIL),
        ).joinToString(", ")

    private fun List<ExtensionResult>.names(): String {
        val names = map { it.name }.sorted()
        val shown = names.take(MAX_NAMES).joinToString()
        return if (names.size > MAX_NAMES) "$shown and ${names.size - MAX_NAMES} more" else shown
    }

    private fun Long?.text(): String = this?.toString() ?: ""

    // Table cells stay on one line, and a pipe in a message would end its cell
    private fun String.cell(): String = replace('\n', ' ').replace("|", "\\|").take(MAX_CELL)

    private const val TOTAL = "Total"
    private const val SLOWEST = 15
    private const val MAX_CELL = 600
    private const val MAX_DETAIL = 300
    private const val MAX_NAMES = 25
    private val COUNTED_ONLY = setOf(OutcomeKind.FAMILY_NOT_ENABLED, OutcomeKind.NOT_INSTALLABLE)
}
