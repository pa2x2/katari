package eu.kanade.tachiyomi.data.backup.conversion.newer

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Kind
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Note
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Row
import tachiyomi.i18n.*

/**
 * The report with what a newer version of [app] wrote (D127): the entries Katari couldn't read left behind in Library,
 * and a row listing each part Katari doesn't know.
 */
internal fun ConversionReport.withNewerApp(findings: NewerAppFindings, app: String): ConversionReport {
    if (findings.isEmpty) return this
    val unreadable = findings.unreadableEntries.map { entry ->
        val name = entry.title?.let(Name::Text)
            ?: Name.Part(MR.strings.backup_conversion_entry_in_backup, listOf(entry.position.toString()))
        Item(name, Note(MR.strings.backup_conversion_newer_entry, listOf(app)))
    }
    val parts = findings.unknownParts.map { part ->
        val note = if (part.times == 1) {
            Note(MR.strings.backup_conversion_newer_part_once)
        } else {
            Note(MR.strings.backup_conversion_newer_part_times, listOf(part.times.toString()))
        }
        Item(Name.NewerPart(part.where, part.name, part.kind), note)
    } + findings.changedParts.map { part ->
        Item(Name.NewerPart(part.where, part.name, part.found), Note(MR.strings.backup_conversion_newer_part_changed))
    }
    val library = rows.firstOrNull { it.kind == Kind.LIBRARY }
    val withLibrary = when {
        unreadable.isEmpty() -> rows
        library == null -> listOf(Row(Kind.LIBRARY, emptyList(), unreadable)) + rows
        else -> rows.map { if (it === library) it.copy(leftBehind = it.leftBehind + unreadable) else it }
    }
    return ConversionReport(withLibrary + listOfNotNull(Row.of(Kind.NEWER_PARTS, leftBehind = parts)))
}
