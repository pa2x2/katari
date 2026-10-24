package mihon.entry.interactions.manga.download

import android.content.ContentResolver
import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.util.lang.Hash.md5
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import logcat.LogPriority
import mihon.entry.interactions.download.SourceDirNames
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.displayablePath
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.*
import java.io.IOException

/**
 * This class is used to provide the directories where the downloads should be saved.
 * It uses the following path scheme: /<root downloads dir>/<source name (LANG)>/<manga>/<chapter>
 *
 * @param context the application context.
 */
internal class DownloadProvider(
    private val context: Context,
    private val storageManager: StorageManager,
    private val libraryPreferences: GlobalLibraryPreferences,
) {

    private val downloadsDir: UniFile?
        get() = storageManager.getDownloadsDirectory()

    /**
     * Returns the download directory for a manga. For internal use only.
     *
     * @param entryTitle the title of the manga to query.
     * @param source the source of the manga.
     */
    internal fun getEntryDir(entryTitle: String, source: UnifiedSource): Result<UniFile> {
        val downloadsDir = downloadsDir
        if (downloadsDir == null) {
            logcat(LogPriority.ERROR) { "Failed to create download directory" }
            return Result.failure(
                IOException(context.stringResource(MR.strings.storage_failed_to_create_download_directory)),
            )
        }

        // An entry downloaded into a legacy source folder keeps its chapters together there
        findEntryDir(entryTitle, source)?.let { return Result.success(it) }

        val sourceDirName = getSourceDirNames(source).current
        val sourceDir = downloadsDir.createDirectory(sourceDirName)
        if (sourceDir == null) {
            val displayablePath = downloadsDir.displayablePath + "/$sourceDirName"
            logcat(LogPriority.ERROR) { "Failed to create source download directory: $displayablePath" }
            return Result.failure(
                IOException(context.stringResource(MR.strings.storage_failed_to_create_directory, displayablePath)),
            )
        }

        val entryDirName = getEntryDirName(entryTitle)
        val entryDir = sourceDir.createDirectory(entryDirName)
        if (entryDir == null) {
            val displayablePath = sourceDir.displayablePath + "/$entryDirName"
            logcat(LogPriority.ERROR) { "Failed to create manga download directory: $displayablePath" }
            return Result.failure(
                IOException(context.stringResource(MR.strings.storage_failed_to_create_directory, displayablePath)),
            )
        }

        return Result.success(entryDir)
    }

    /** Returns the existing download directories of a source, the one under its current name first. */
    fun findSourceDirs(source: UnifiedSource): List<UniFile> {
        val downloadsDir = downloadsDir ?: return emptyList()
        return getSourceDirNames(source).all.mapNotNull(downloadsDir::findFile)
    }

    /** Returns the download directory named [sourceDirName] if it exists. */
    fun findSourceDir(sourceDirName: String): UniFile? {
        return downloadsDir?.findFile(sourceDirName)
    }

    /**
     * Returns the download directory for a manga if it exists, preferring the one in the source's current directory.
     *
     * @param entryTitle the title of the manga to query.
     * @param source the source of the manga.
     */
    fun findEntryDir(entryTitle: String, source: UnifiedSource): UniFile? {
        val sourceDirs = findSourceDirs(source).ifEmpty { return null }
        val entryDirName = getEntryDirName(entryTitle)
        return sourceDirs.firstNotNullOfOrNull { it.findFile(entryDirName) }
    }

    /**
     * Returns the download directory for a chapter if it exists.
     *
     * @param chapterName the name of the chapter to query.
     * @param chapterScanlator scanlator of the chapter to query
     * @param entryTitle the title of the manga to query.
     * @param source the source of the chapter.
     */
    fun findChapterDir(
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        entryTitle: String,
        source: UnifiedSource,
    ): UniFile? {
        val entryDir = findEntryDir(entryTitle, source)
        return getValidChapterDirNames(chapterName, chapterScanlator, chapterUrl)
            .firstNotNullOfOrNull { entryDir?.findFile(it) }
    }

    /**
     * Returns a list of downloaded directories for the chapters that exist.
     *
     * @param chapters the chapters to query.
     * @param entry the manga of the chapter.
     * @param source the source of the chapter.
     */
    suspend fun findChapterDirs(
        chapters: List<EntryChapter>,
        entry: Entry,
        source: UnifiedSource,
    ): Pair<UniFile?, List<UniFile>> = withIOContext {
        val entryDir = findEntryDir(entry.downloadFolderTitle, source) ?: return@withIOContext null to emptyList()
        entryDir to findChapterDirs(chapters, entryDir).values.toList()
    }

    /**
     * Returns the entry's download directory and the download of each of [chapters] in it, keyed by chapter id, or
     * `null` when the entry has no download directory.
     */
    suspend fun findChapterDirsById(
        chapters: List<EntryChapter>,
        entry: Entry,
        source: UnifiedSource,
    ): Pair<UniFile, Map<Long, UniFile>>? = withIOContext {
        val entryDir = findEntryDir(entry.downloadFolderTitle, source) ?: return@withIOContext null
        entryDir to findChapterDirs(chapters, entryDir)
    }

    /**
     * Returns the download of each chapter that has one, keyed by chapter id.
     *
     * A chapter is looked for under every name it may have been downloaded with, with the chapters looked up in
     * parallel. On a storage access framework directory each lookup is a content resolver query, so from
     * [LIST_CHAPTER_DIRS_MIN_CHAPTERS] chapters on the directory is listed once instead and the names are matched
     * against the listing, ignoring case as the lookups do. Plain file directories are always looked up by name, as
     * their listing can leave out files another app created.
     */
    private suspend fun findChapterDirs(chapters: List<EntryChapter>, entryDir: UniFile): Map<Long, UniFile> {
        val disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get()
        val appendHash = libraryPreferences.enableChapterNameHash.get()
        fun EntryChapter.findDir(find: (String) -> UniFile?): UniFile? {
            return ChapterDirNames(name, scanlator, url).validDirNames(disallowNonAscii, appendHash)
                .firstNotNullOfOrNull(find)
        }

        val isDocumentTree = entryDir.uri.scheme == ContentResolver.SCHEME_CONTENT
        if (isDocumentTree && chapters.size >= LIST_CHAPTER_DIRS_MIN_CHAPTERS) {
            val chapterDirs = entryDir.listFiles().orEmpty()
                .filter { it.name != null }
                .associateByTo(sortedMapOf(String.CASE_INSENSITIVE_ORDER)) { it.name!! }
            return chapters
                .mapNotNull { chapter -> chapter.findDir { chapterDirs[it] }?.let { chapter.id to it } }
                .toMap()
        }
        return coroutineScope {
            chapters
                .map { chapter ->
                    async(chapterDirLookupDispatcher) { chapter.findDir(entryDir::findFile)?.let { chapter.id to it } }
                }
                .awaitAll()
                .filterNotNull()
                .toMap()
        }
    }

    /**
     * Returns the name of the stored translation of the downloaded chapter [chapterArtifact] (its folder or archive).
     *
     * The name is derived from the artifact actually found, so chapters stored under a legacy name keep their
     * translation, and it is a hidden file of fixed length, so long chapter names stay within the filename limit and it
     * is never taken for a chapter.
     */
    fun getChapterTranslationFileName(chapterArtifact: UniFile): String =
        getChapterTranslationFileName(checkNotNull(chapterArtifact.name) { "Chapter artifact has no name" })

    /** Returns the name of the stored translation of the downloaded chapter named [chapterArtifactName]. */
    fun getChapterTranslationFileName(chapterArtifactName: String): String =
        ".translation-" + md5(chapterArtifactName.removeSuffix(".cbz")) + ".json"

    /** Returns the name a stored translation of [chapterArtifact] is written under before it replaces the last one. */
    fun getChapterTranslationTemporaryFileName(chapterArtifact: UniFile): String =
        getChapterTranslationFileName(chapterArtifact) + ".tmp"

    /**
     * `<name> (<LANG>)`, as Mihon names it, so the downloads in a Mihon storage folder are found. Katari 1.15 and
     * earlier wrote the legacy `<name>`.
     */
    fun getSourceDirNames(source: UnifiedSource): SourceDirNames {
        val disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get()
        val legacy = DiskUtil.buildValidFilename(source.name, disallowNonAscii = disallowNonAscii)
        val language = (source as? EntryCatalogueSource)?.lang ?: (source as? UnifiedStubSource)?.lang
        if (language.isNullOrBlank()) return SourceDirNames(current = legacy, legacy = null)
        val current = DiskUtil.buildValidFilename(
            "${source.name} (${language.uppercase()})",
            disallowNonAscii = disallowNonAscii,
        )
        return SourceDirNames(current = current, legacy = legacy)
    }

    /**
     * Returns the download directory name for a manga.
     *
     * @param entryTitle the title of the manga to query.
     */
    fun getEntryDirName(entryTitle: String): String {
        return DiskUtil.buildValidFilename(
            entryTitle,
            disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get(),
        )
    }

    /** Returns the name a new download of the chapter is written under. */
    fun getChapterDirName(chapterName: String, chapterScanlator: String?, chapterUrl: String): String {
        return ChapterDirNames(chapterName, chapterScanlator, chapterUrl).dirName(
            disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get(),
            appendHash = libraryPreferences.enableChapterNameHash.get(),
        )
    }

    /** Returns every name the chapter's download may have on disk, the one [getChapterDirName] returns first. */
    fun getValidChapterDirNames(chapterName: String, chapterScanlator: String?, chapterUrl: String): List<String> {
        return ChapterDirNames(chapterName, chapterScanlator, chapterUrl).validDirNames(
            disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get(),
            appendHash = libraryPreferences.enableChapterNameHash.get(),
        )
    }
}

private const val LIST_CHAPTER_DIRS_MIN_CHAPTERS = 50

private const val CHAPTER_DIR_LOOKUP_PARALLELISM = 8

private val chapterDirLookupDispatcher = Dispatchers.IO.limitedParallelism(CHAPTER_DIR_LOOKUP_PARALLELISM)
