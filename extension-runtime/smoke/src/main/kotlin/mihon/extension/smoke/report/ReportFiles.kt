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
 * A folder of reports, each run's as `smoke-<date>T<time>.md` and `.json`. The new Markdown says what changed since an
 * earlier report: the one asked for, or else the previous one, which is the last JSON before the new one's, as the
 * names sort by time.
 */
internal class ReportFiles(private val directory: Path) {

    data class Written(val markdown: Path, val json: Path)

    /** [compareWith] is the earlier report's JSON, null for the previous one in the folder. */
    fun write(results: SmokeResults, compareWith: Path? = null): Written {
        directory.createDirectories()
        val baseName = "$PREFIX${results.startedAt.replace(':', '-')}"
        val json = directory.resolve("$baseName.json")
        // The results first, so an earlier report this version can't read doesn't lose them
        json.writeText(format.encodeToString(SmokeResults.serializer(), results))
        val earlier = if (compareWith != null) compareWith.name to read(compareWith) else previous(before = json.name)
        val diff = earlier?.let { (name, before) -> ReportDiff.between(name, before, results) }
        val markdown = directory.resolve("$baseName.md")
        markdown.writeText(SmokeReport.markdown(results, diff))
        return Written(markdown, json)
    }

    /** The latest report before the JSON file [before], passing over any this version can't read. */
    private fun previous(before: String): Pair<String, SmokeResults>? = directory.listDirectoryEntries("$PREFIX*.json")
        .filter { it.isRegularFile() && it.name < before }
        .sortedByDescending { it.name }
        .firstNotNullOfOrNull { file ->
            runCatching { file.name to read(file) }.getOrNull()
        }

    private fun read(file: Path): SmokeResults = format.decodeFromString(SmokeResults.serializer(), file.readText())

    private companion object {
        const val PREFIX = "smoke-"
        val format = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
