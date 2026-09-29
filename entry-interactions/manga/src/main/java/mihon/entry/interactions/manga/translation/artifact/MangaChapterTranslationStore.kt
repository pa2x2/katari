package mihon.entry.interactions.manga.translation.artifact

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.entry.interactions.manga.download.DownloadProvider
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import java.io.IOException

/** Stored translations of downloaded chapters, kept beside the chapter download they were made from. */
internal class MangaChapterTranslationStore(
    private val provider: DownloadProvider,
) {
    /** The chapter's stored translation, or `null` when it has none or it cannot be read. */
    suspend fun read(chapter: EntryChapter, entry: Entry, source: UnifiedSource): MangaChapterTranslation? =
        withIOContext {
            val (entryDir, artifact) = locate(chapter, entry, source) ?: return@withIOContext null
            val file = entryDir.findFile(provider.getChapterTranslationFileName(artifact))
                ?.takeIf { it.isFile }
                ?: return@withIOContext null
            val text = try {
                file.openInputStream().use { it.readBytes().decodeToString() }
            } catch (_: IOException) {
                return@withIOContext null
            }
            MangaChapterTranslationFormat.decode(text)
        }

    /**
     * Replaces the chapter's stored translation. The file is written under a temporary name first, so an interrupted
     * write never leaves a file that reads as a translation.
     *
     * @throws IOException when the chapter is not downloaded or the file cannot be written.
     */
    suspend fun write(
        chapter: EntryChapter,
        entry: Entry,
        source: UnifiedSource,
        translation: MangaChapterTranslation,
    ) = withIOContext {
        val (entryDir, artifact) = locate(chapter, entry, source)
            ?: throw IOException("Chapter ${chapter.id} is not downloaded")
        val name = provider.getChapterTranslationFileName(artifact)
        val temporaryName = provider.getChapterTranslationTemporaryFileName(artifact)
        entryDir.findFile(temporaryName)?.delete()
        val temporary = entryDir.createFile(temporaryName)
            ?: throw IOException("Could not create $temporaryName")
        try {
            val encoded = MangaChapterTranslationFormat.encode(translation).encodeToByteArray()
            temporary.openOutputStream().use { it.write(encoded) }
            entryDir.findFile(name)?.delete()
            if (!temporary.renameTo(name)) throw IOException("Could not rename $temporaryName")
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    /** Deletes the chapter's stored translation and keeps its download. */
    suspend fun delete(chapter: EntryChapter, entry: Entry, source: UnifiedSource) {
        withIOContext {
            val (entryDir, artifact) = locate(chapter, entry, source) ?: return@withIOContext
            entryDir.findFile(provider.getChapterTranslationFileName(artifact))?.delete()
            entryDir.findFile(provider.getChapterTranslationTemporaryFileName(artifact))?.delete()
        }
    }

    /** The entry's download folder and the chapter's folder or archive in it, if the chapter is downloaded. */
    private fun locate(chapter: EntryChapter, entry: Entry, source: UnifiedSource): Pair<UniFile, UniFile>? {
        val (entryDir, artifacts) = provider.findChapterDirs(listOf(chapter), entry, source)
        val artifact = artifacts.firstOrNull() ?: return null
        return entryDir?.let { it to artifact }
    }
}
