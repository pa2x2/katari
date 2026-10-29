package mihon.extension.smoke.report

import kotlinx.serialization.json.Json
import mihon.extension.smoke.results.SmokeResults
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * A folder of reports, each run's as `smoke-<date>T<time>.md` and `.json`. The names sort by time, so the previous
 * report is the last JSON before the new one's; the new Markdown says what changed since it.
 */
internal class ReportFiles(private val directory: Path) {

    data class Written(val markdown: Path, val json: Path)

    fun write(results: SmokeResults): Written {
        directory.createDirectories()
        val baseName = "$PREFIX${results.startedAt.replace(':', '-')}"
        val json = directory.resolve("$baseName.json")
        val diff = previous(before = json.name)?.let { (name, earlier) -> ReportDiff.between(name, earlier, results) }
        json.writeText(format.encodeToString(SmokeResults.serializer(), results))
        val markdown = directory.resolve("$baseName.md")
        markdown.writeText(SmokeReport.markdown(results, diff))
        return Written(markdown, json)
    }

    /** The latest report before the JSON file [before], passing over any this version can't read. */
    private fun previous(before: String): Pair<String, SmokeResults>? = directory.listDirectoryEntries("$PREFIX*.json")
        .filter { it.isRegularFile() && it.name < before }
        .sortedByDescending { it.name }
        .firstNotNullOfOrNull { file ->
            runCatching { file.name to format.decodeFromString(SmokeResults.serializer(), file.readText()) }.getOrNull()
        }

    private companion object {
        const val PREFIX = "smoke-"
        val format = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
