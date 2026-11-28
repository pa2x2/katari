package app.katari.backupconversion.issue

import app.katari.domain.app.AppIdentity
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.debug.DeviceDiagnostics
import eu.kanade.tachiyomi.data.backup.conversion.BackupFileContents
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport
import eu.kanade.tachiyomi.data.backup.conversion.newer.NewerAppFindings
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import tachiyomi.core.common.Constants
import java.net.URLEncoder
import kotlin.time.Clock

/**
 * The issues Bring over opens on Katari's GitHub (D130-D134): Report for a backup Katari couldn't fully read, Ask for
 * a file no app Katari reads wrote. Everything Katari found goes in the file the person attaches; the link fills in
 * only a summary, which always fits in it (D132). Nothing from the library goes in either: the issue is public (D131).
 */
@Inject
internal class BackupIssues(
    private val identity: AppIdentity,
    private val diagnostics: DeviceDiagnostics,
) {
    /** Report: [app] read the file at [file] with [result]; [detected] whether Katari named the app itself. */
    fun report(app: String, detected: Boolean, file: FileFacts, result: ReportedResult): BackupIssue {
        val head = listOf(readAs(app, detected), file.line) + build()
        val found = when (result) {
            is ReportedResult.Converted -> converted(result.findings, result.report)
            ReportedResult.NewerLayout -> Found(
                summary = listOf("Result: the backup was made by a newer $app than this Katari reads"),
                details = emptyList(),
            )
            is ReportedResult.Failed -> Found(
                summary = listOf("Result: converting failed: ${result.error}"),
                details = listOf("Stack trace:", result.error.stackTraceToString().trimEnd()),
            )
        }
        val title = when (result) {
            is ReportedResult.Converted -> "$app backup: parts Katari doesn't read"
            ReportedResult.NewerLayout -> "$app backup: made by a newer $app"
            is ReportedResult.Failed -> "$app backup: converting failed"
        }
        return issue(REPORT_FORM, title, head + found.summary, found.details, filePrefix = app)
    }

    /** Ask: no app Katari reads wrote the file at [file], which holds [contents]. */
    fun ask(file: FileFacts, contents: BackupFileContents): BackupIssue {
        val layout = when (contents) {
            is BackupFileContents.Protobuf -> listOf(
                "Kind: protobuf",
                "Top-level fields: ${contents.fieldNumbers.sorted().joinToString()}",
            )
            is BackupFileContents.Zip -> listOf(
                "Kind: zip",
                "Entries: ${contents.entryNames.sorted().joinToString().take(ENTRY_NAMES_IN_LINK)}",
            )
            BackupFileContents.Json -> listOf("Kind: JSON")
        }
        val details = (contents as? BackupFileContents.Zip)?.let { zip ->
            listOf("All entries:") + zip.entryNames.sorted().map { "- $it" }
        }.orEmpty()
        return issue(ASK_FORM, "Bring over from another app", listOf(file.line) + layout + build(), details, null)
    }

    private fun issue(
        form: String,
        title: String,
        summary: List<String>,
        details: List<String>,
        filePrefix: String?,
    ): BackupIssue {
        val findings = (summary + listOf("") + details).joinToString("\n").trimEnd() + "\n"
        val found = (summary + "The details are in the attached file.").joinToString("\n")
        val url = "${Constants.URL_REPOSITORY}/issues/new?template=$form" +
            "&title=${encode(title)}&found=${encode(found)}"
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val app = filePrefix?.lowercase()?.filter(Char::isLetterOrDigit)
        val name = listOfNotNull("katari-report", app, today.toString()).joinToString("-") + ".zip"
        return BackupIssue(findings, url, name)
    }

    private fun converted(findings: NewerAppFindings, report: ConversionReport): Found {
        val settings = report.unknownSettings
        val written = listOfNotNull(
            findings.appVersion?.let { "app version $it" },
            findings.formatVersion?.let { "format $it" },
        )
        val summary = listOfNotNull(
            written.takeIf { it.isNotEmpty() }?.let { "Written as: ${it.joinToString()}" },
            "Found: ${findings.unknownParts.size + findings.changedParts.size} parts Katari doesn't know, " +
                "${findings.unreadableEntries.size} entries it couldn't read, " +
                "${settings.size} settings it doesn't know",
        )
        val details = buildList {
            if (findings.unknownParts.isNotEmpty()) {
                add("Parts Katari doesn't know:")
                findings.unknownParts.forEach {
                    add("- ${it.where.joinToString(" › ")} field ${it.name} (${it.kind}), found ${times(it.times)}")
                }
            }
            if (findings.changedParts.isNotEmpty()) {
                add("Parts holding another kind of value:")
                findings.changedParts.forEach {
                    add(
                        "- ${it.where.joinToString(" › ")} field ${it.name}: expected ${it.expected}, " +
                            "found ${it.found}, ${times(it.times)}",
                    )
                }
            }
            if (findings.unreadableEntries.isNotEmpty()) {
                add("Entries that couldn't be read:")
                findings.unreadableEntries.forEach { add("- entry ${it.position}: ${it.reason}") }
            }
            if (settings.isNotEmpty()) {
                add("Settings Katari doesn't know:")
                settings.forEach { add("- $it") }
            }
        }
        return Found(summary, details)
    }

    private fun times(times: Int) = if (times == 1) "once" else "$times times"

    private fun readAs(app: String, detected: Boolean) =
        "Read as: $app (${if (detected) "detected from the file" else "chosen by the person"})"

    private fun build(): List<String> = listOf(
        "Katari: ${identity.versionName} (${identity.commitSha}) · ${identity.applicationId} · " +
            identity.platformName,
    ) + diagnostics.deviceFacts().map { "${it.title}: ${it.value}" }

    private class Found(val summary: List<String>, val details: List<String>)

    private fun encode(text: String): String = URLEncoder.encode(text, Charsets.UTF_8)

    private companion object {
        const val REPORT_FORM = "4_backup_from_another_app.yml"
        const val ASK_FORM = "5_bring_over_another_app.yml"

        // A zip of thousands of files lists them all in the attached file, so the link stays short enough to open
        const val ENTRY_NAMES_IN_LINK = 500
    }
}

/** What an issue's file holds ([findings]), the link opening the form, and the name offered for the file. */
internal class BackupIssue(val findings: String, val url: String, val fileName: String)

/** The chosen file as the issue names it: its name and size, where its storage says. */
internal class FileFacts(name: String?, size: Long?) {
    val line = "File: ${name ?: "(no name)"}${size?.let { " · $it bytes" }.orEmpty()}"
}

/** What converting a file came to, as Report tells it. */
internal sealed interface ReportedResult {
    /** Converted, with what a newer app wrote, and the report the settings Katari doesn't know are listed in. */
    class Converted(val findings: NewerAppFindings, val report: ConversionReport) : ReportedResult

    /** The file is a newer app's, laid out as Katari doesn't read (D129). */
    data object NewerLayout : ReportedResult

    class Failed(val error: Throwable) : ReportedResult
}
