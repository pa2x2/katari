package mihon.entry.interactions.anime.download.staging

import com.hippo.unifile.UniFile
import tachiyomi.core.common.storage.ListedFile
import java.io.Closeable
import java.io.IOException
import java.io.OutputStream
import java.util.TreeMap
import java.util.TreeSet

/**
 * An episode's staging folder, which a download fills and then publishes under the episode's name. It outlives an
 * attempt that fails or is interrupted, so the next one can keep the segments it completed: [completedFiles] gives
 * them while the stream still plans the same files, and [recordCompleted] records each file as it completes.
 *
 * Every file an attempt writes goes through [newFile], which replaces what an earlier attempt left under that name.
 * [finish] then removes the rest of what earlier attempts left, and the journal, so the folder holds this download
 * only. Files may be created and recorded from several coroutines at once.
 */
internal class StagingFolder(
    val directory: UniFile,
    /** The folder's files when this attempt began, by name regardless of case. */
    earlierFiles: Map<String, ListedFile>,
) : Closeable {
    private val earlier = TreeMap<String, ListedFile>(String.CASE_INSENSITIVE_ORDER).apply { putAll(earlierFiles) }
    private val claimed = TreeSet(String.CASE_INSENSITIVE_ORDER)
    private val journalText: String = earlier[ResumeJournal.FILE_NAME]?.file?.let { file ->
        runCatching { file.openInputStream().use { it.readBytes().decodeToString() } }.getOrNull()
    }.orEmpty()
    private var journalFile: UniFile? = null
    private var journal: OutputStream? = null

    /**
     * The files of [part] an earlier attempt completed under the same plan, by name with their size, which this
     * attempt keeps. Under a different plan none are, and [part] starts over.
     */
    @Synchronized
    fun completedFiles(part: String, planId: String): Map<String, Long> {
        val recorded = ResumeJournal.completed(journalText, part, planId)
            ?: return emptyMap<String, Long>().also { append(ResumeJournal.planLine(part, planId)) }
        val kept = recorded.filter { (name, size) -> size > 0 && earlier[name]?.length == size }
        claimed += kept.keys
        return kept
    }

    @Synchronized
    fun recordCompleted(part: String, name: String, size: Long) {
        append(ResumeJournal.doneLine(part, name, size))
    }

    /** A new, empty file in the folder named exactly [name]. */
    fun newFile(name: String): UniFile {
        val leftOver = synchronized(this) {
            claimed += name
            earlier.remove(name)
        }
        if (leftOver != null && !leftOver.file.delete()) throw IOException("Couldn't replace '$name'")
        return directory.createNamedFile(name)
    }

    /** Removes what earlier attempts left that this one neither kept nor wrote, and the journal. */
    @Synchronized
    fun finish() {
        close()
        earlier.filterKeys { it !in claimed }.values.forEach { it.file.delete() }
        journalFile?.delete()
    }

    @Synchronized
    override fun close() {
        journal?.close()
        journal = null
    }

    private fun append(line: String) {
        val output = journal ?: openJournal().also { journal = it }
        // One write per record, unbuffered, so a killed process leaves whole records and at most one partial line.
        output.write(line.toByteArray())
        output.flush()
    }

    private fun openJournal(): OutputStream {
        val file = earlier.remove(ResumeJournal.FILE_NAME)?.file ?: directory.createNamedFile(ResumeJournal.FILE_NAME)
        claimed += ResumeJournal.FILE_NAME
        journalFile = file
        return file.openOutputStream(true)
    }
}

/**
 * A new file in [this] folder named exactly [name]. A playlist or manifest written beside it names its files, so one
 * the storage provider renamed (to avoid a clash, or to suit a MIME type) fails the download here instead of leaving an
 * episode that can't play.
 */
private fun UniFile.createNamedFile(name: String): UniFile {
    val file = createFile(name) ?: throw IOException("Couldn't create '$name'")
    if (file.name != name) {
        file.delete()
        throw IOException("The storage named '$name' '${file.name}'")
    }
    return file
}
