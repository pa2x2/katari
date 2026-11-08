package mihon.entry.interactions.anime.download.staging

/**
 * The record a staging folder keeps of the files its segmented downloads completed, so a later attempt can tell a
 * whole segment from one an interruption cut short: a file is recorded only once all of it is written. Records are
 * lines appended as they happen, so an attempt killed while writing one loses only that line:
 *
 * ```
 * plan <part> <plan id>        a download of <part>, such as `video.m3u`, planned these files
 * done <part> <name> <size>    <name> of that plan is complete, <size> bytes long
 * ```
 *
 * Only the records after a part's latest plan line count, so a part whose plan changed starts over.
 */
internal object ResumeJournal {
    const val FILE_NAME = "resume_journal.txt"

    /**
     * The files of [part] that [journal] records as complete, by name with their size; null unless the part's latest
     * plan is [planId].
     */
    fun completed(journal: String, part: String, planId: String): Map<String, Long>? {
        var completed: MutableMap<String, Long>? = null
        // A last line without its line break was cut short.
        for (line in journal.substringBeforeLast('\n', missingDelimiterValue = "").lines()) {
            val fields = line.split(SEPARATOR)
            when {
                fields.getOrNull(1) != part -> continue
                fields[0] == PLAN && fields.size == 3 -> completed = if (fields[2] == planId) HashMap() else null
                fields[0] == DONE && fields.size == 4 -> fields[3].toLongOrNull()?.let { completed?.put(fields[2], it) }
            }
        }
        return completed
    }

    fun planLine(part: String, planId: String): String = record(PLAN, part, planId)

    fun doneLine(part: String, name: String, size: Long): String = record(DONE, part, name, size.toString())

    private fun record(vararg fields: String): String {
        require(fields.none { SEPARATOR in it || '\n' in it }) { "Journal field with a separator: ${fields.toList()}" }
        return fields.joinToString(SEPARATOR, postfix = "\n")
    }

    private const val SEPARATOR = "\t"
    private const val PLAN = "plan"
    private const val DONE = "done"
}
