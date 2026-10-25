package mihon.entry.interactions.anime.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.serialization.json.Json
import logcat.LogPriority
import mihon.entry.interactions.anime.download.model.AnimeDownloadManifest
import mihon.entry.interactions.anime.download.model.DownloadedArtifact
import mihon.entry.interactions.anime.download.model.EpisodePackage
import mihon.entry.interactions.anime.download.staging.StagingFolder
import mihon.entry.interactions.catalogue.EntryCatalogueSourceInfo
import mihon.entry.interactions.download.SourceDirNames
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.DirectoryListing
import tachiyomi.core.common.storage.ListedFile
import tachiyomi.core.common.storage.displayablePath
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.*
import java.io.IOException
import java.io.InputStream
import java.util.TreeMap

internal class AnimeDownloadProvider(
    private val context: Context,
    private val storageManager: StorageManager,
    private val libraryPreferences: GlobalLibraryPreferences,
    private val json: Json,
) {

    private val directoryListing = DirectoryListing(context)

    private val downloadsDir: UniFile?
        get() = storageManager.getDownloadsDirectory()

    internal fun getAnimeDir(animeTitle: String, source: UnifiedSource): Result<UniFile> {
        val downloadsDir = downloadsDir
        if (downloadsDir == null) {
            logcat(LogPriority.ERROR) { "Failed to create anime download directory" }
            return Result.failure(
                IOException(context.stringResource(MR.strings.storage_failed_to_create_download_directory)),
            )
        }

        // An anime downloaded into a legacy source folder keeps its episodes together there
        findAnimeDir(animeTitle, source)?.let { return Result.success(it) }

        val sourceDirName = getSourceDirNames(source).current
        val sourceDir = downloadsDir.createDirectory(sourceDirName)
        if (sourceDir == null) {
            val displayablePath = downloadsDir.displayablePath + "/$sourceDirName"
            logcat(LogPriority.ERROR) { "Failed to create anime source download directory: $displayablePath" }
            return Result.failure(
                IOException(context.stringResource(MR.strings.storage_failed_to_create_directory, displayablePath)),
            )
        }

        val animeDirName = getAnimeDirName(animeTitle)
        val animeDir = sourceDir.createDirectory(animeDirName)
        if (animeDir == null) {
            val displayablePath = sourceDir.displayablePath + "/$animeDirName"
            logcat(LogPriority.ERROR) { "Failed to create anime title download directory: $displayablePath" }
            return Result.failure(
                IOException(context.stringResource(MR.strings.storage_failed_to_create_directory, displayablePath)),
            )
        }

        return Result.success(animeDir)
    }

    /** Returns the existing download directories of a source, the one under its current name first. */
    fun findSourceDirs(source: UnifiedSource): List<UniFile> {
        val downloadsDir = downloadsDir ?: return emptyList()
        return getSourceDirNames(source).all.mapNotNull(downloadsDir::findFile)
    }

    /** Returns the download directory of an anime if it exists, preferring the one in the source's current directory. */
    fun findAnimeDir(animeTitle: String, source: UnifiedSource): UniFile? {
        val animeDirName = getAnimeDirName(animeTitle)
        return findSourceDirs(source).firstNotNullOfOrNull { it.findFile(animeDirName) }
    }

    fun findEpisodePackage(
        episodeName: String,
        episodeUrl: String,
        animeTitle: String,
        source: UnifiedSource,
    ): EpisodePackage? {
        val animeDir = findAnimeDir(animeTitle, source) ?: return null
        return getValidEpisodeDirNames(episodeName, episodeUrl).asSequence()
            .mapNotNull(animeDir::findFile)
            .firstNotNullOfOrNull(::readEpisodePackage)
    }

    fun findEpisodeDirs(
        episodes: List<EntryChapter>,
        anime: Entry,
        source: UnifiedSource,
    ): Pair<UniFile?, List<UniFile>> {
        val animeDir = findAnimeDir(anime.downloadFolderTitle, source) ?: return null to emptyList()
        return animeDir to episodes.mapNotNull { episode ->
            getValidEpisodeDirNames(episode.name, episode.url).asSequence()
                .mapNotNull { animeDir.findFile(it) }
                .firstOrNull()
        }
    }

    fun getSourceDirNames(source: UnifiedSource): SourceDirNames {
        val language = (source as? EntryCatalogueSource)?.lang ?: (source as? UnifiedStubSource)?.lang
        return animeSourceDirNames(source.name, language, disallowNonAscii)
    }

    fun getSourceDirNames(sources: List<EntryCatalogueSourceInfo>): Map<Long, SourceDirNames> {
        return sources.animeSourceDirNames(disallowNonAscii)
    }

    private val disallowNonAscii: Boolean
        get() = libraryPreferences.disallowNonAsciiFilenames.get()

    fun getAnimeDirName(animeTitle: String): String {
        return DiskUtil.buildValidFilename(
            animeTitle,
            disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get(),
        )
    }

    fun getEpisodeDirName(episodeName: String, episodeUrl: String): String {
        return EpisodeDirNames(episodeName, episodeUrl).dirName(
            disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get(),
            appendHash = libraryPreferences.enableChapterNameHash.get(),
        )
    }

    /** Returns every name the episode's download may have on disk, the one [getEpisodeDirName] returns first. */
    fun getValidEpisodeDirNames(episodeName: String, episodeUrl: String): List<String> {
        return EpisodeDirNames(episodeName, episodeUrl).validDirNames(
            disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get(),
            appendHash = libraryPreferences.enableChapterNameHash.get(),
        )
    }

    fun createArtifactRecords(directory: UniFile): List<DownloadedArtifact> {
        return listFilesByName(directory).values
            .filter { it.name != AnimeDownloader.MANIFEST_FILE_NAME }
            .map { file ->
                DownloadedArtifact(
                    fileName = checkNotNull(file.name),
                    storedSize = file.length,
                )
            }
    }

    /**
     * The downloaded episode in [directory], read from one listing of it, or null when the folder isn't one or is
     * broken: a package with a manifest must hold every file the manifest names, each at the size it was downloaded
     * with, so a package missing a segment isn't played as if it were whole.
     */
    fun readEpisodePackage(directory: UniFile): EpisodePackage? {
        val files = listFilesByName(directory)
        val manifestFile = files[AnimeDownloader.MANIFEST_FILE_NAME]
        if (manifestFile == null) {
            val hasLegacyVideo = files.keys.any { name ->
                LEGACY_VIDEO_EXTENSIONS.any { name.endsWith(it, ignoreCase = true) }
            }
            return EpisodePackage(manifest = null, files = files).takeIf { hasLegacyVideo }
        }
        return readValidManifest(manifestFile, files)?.let { EpisodePackage(it, files) }
    }

    /** Files by name, matched regardless of case as shared storage matches them. */
    private fun listFilesByName(directory: UniFile): Map<String, ListedFile> {
        return directoryListing.list(directory)
            .filter { !it.isDirectory && it.name != null }
            .associateByTo(TreeMap(String.CASE_INSENSITIVE_ORDER)) { checkNotNull(it.name) }
    }

    private fun readValidManifest(
        manifestFile: ListedFile,
        files: Map<String, ListedFile>,
    ): AnimeDownloadManifest? = runCatching {
        require(manifestFile.length != 0L)
        val manifest = manifestFile.file.openInputStream().use {
            json.decodeFromString<AnimeDownloadManifest>(it.readBoundedText(MAX_MANIFEST_BYTES))
        }
        fun requireReferencedArtifact(fileName: String): ListedFile {
            require(fileName.isSafeArtifactName())
            return files[fileName]?.takeIf { it.length != 0L }
                ?: throw IOException("Downloaded anime artifact $fileName is missing or empty")
        }
        requireReferencedArtifact(manifest.video.fileName)
        manifest.subtitles.forEach { requireReferencedArtifact(it.fileName) }
        manifest.audioTracks.forEach { requireReferencedArtifact(it.fileName) }
        val referencedNames = buildSet {
            add(manifest.video.fileName)
            manifest.subtitles.mapTo(this) { it.fileName }
            manifest.audioTracks.mapTo(this) { it.fileName }
        }
        val artifactNames = manifest.artifacts.map { it.fileName }
        require(artifactNames.distinct().size == artifactNames.size)
        if (artifactNames.isNotEmpty()) require(artifactNames.containsAll(referencedNames))
        manifest.artifacts.forEach { artifact ->
            require(requireReferencedArtifact(artifact.fileName).length == artifact.storedSize)
        }
        manifest
    }.getOrNull()

    fun isEpisodePackageValid(directory: UniFile): Boolean = readEpisodePackage(directory) != null

    fun publishEpisodePackage(
        animeDirectory: UniFile,
        stagingDirectory: UniFile,
        finalDirectoryName: String,
    ): Result<UniFile> = runCatching {
        require(readEpisodePackage(stagingDirectory)?.manifest != null) { "Anime staging package failed validation" }
        val backupName = finalDirectoryName + BACKUP_DIR_SUFFIX
        animeDirectory.findFile(backupName)?.delete()
        val existing = animeDirectory.findFile(finalDirectoryName)
        if (existing != null && !existing.renameTo(backupName)) {
            throw IOException("Unable to preserve the existing anime download")
        }
        if (!stagingDirectory.renameTo(finalDirectoryName)) {
            animeDirectory.findFile(backupName)?.renameTo(finalDirectoryName)
            throw IOException("Unable to publish the anime download")
        }
        val published = animeDirectory.findFile(finalDirectoryName)
        if (published == null || readEpisodePackage(published)?.manifest == null) {
            published?.delete()
            animeDirectory.findFile(backupName)?.renameTo(finalDirectoryName)
            throw IOException("Published anime download failed validation")
        }
        animeDirectory.findFile(backupName)?.delete()
        published
    }

    /** The episode's staging folder, with what an earlier attempt to download it left there. */
    fun beginEpisodePackage(animeDirectory: UniFile, finalDirectoryName: String): Result<StagingFolder> = runCatching {
        recoverEpisodePackage(animeDirectory, finalDirectoryName)
        val stagingName = finalDirectoryName + AnimeDownloadManager.TMP_DIR_SUFFIX
        val directory = animeDirectory.findFile(stagingName)?.takeIf { it.isDirectory }
            ?: animeDirectory.createDirectory(stagingName)
            ?: throw IOException("Unable to create temporary anime download directory")
        StagingFolder(directory, listFilesByName(directory))
    }

    /** Deletes what attempts to download the episode left in its staging folder, once it is no longer to be downloaded. */
    fun discardEpisodePackage(episodeName: String, episodeUrl: String, animeTitle: String, source: UnifiedSource) {
        val animeDirectory = findAnimeDir(animeTitle, source) ?: return
        getValidEpisodeDirNames(episodeName, episodeUrl)
            .mapNotNull { animeDirectory.findFile(it + AnimeDownloadManager.TMP_DIR_SUFFIX) }
            .forEach { it.delete() }
    }

    fun recoverEpisodePackages(animeDirectory: UniFile) {
        animeDirectory.listFiles().orEmpty()
            .mapNotNull { it.name }
            .filter { it.endsWith(BACKUP_DIR_SUFFIX) }
            .map { it.removeSuffix(BACKUP_DIR_SUFFIX) }
            .forEach { recoverEpisodePackage(animeDirectory, it) }
    }

    /**
     * Renames a source's folders after the source is renamed. A legacy folder is renamed too, so a source whose
     * downloads are still there, maybe together with the other languages of its extension, still finds them.
     */
    fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource): Boolean {
        val root = downloadsDir ?: return false
        val oldNames = getSourceDirNames(oldSource)
        val newNames = getSourceDirNames(newSource)
        val renamedCurrent = renameDirectory(root, oldNames.current, newNames.current)
        val oldLegacy = oldNames.legacy
        val newLegacy = newNames.legacy
        val renamedLegacy = oldLegacy != null && newLegacy != null && renameDirectory(root, oldLegacy, newLegacy)
        return renamedCurrent || renamedLegacy
    }

    fun renameEntry(source: UnifiedSource, entry: Entry, newTitle: String): Boolean {
        val oldName = getAnimeDirName(entry.downloadFolderTitle)
        val sourceDirectory = findSourceDirs(source).firstOrNull { it.findFile(oldName) != null } ?: return false
        return renameDirectory(sourceDirectory, oldName, getAnimeDirName(newTitle))
    }

    private fun recoverEpisodePackage(animeDirectory: UniFile, finalDirectoryName: String) {
        val backup = animeDirectory.findFile(finalDirectoryName + BACKUP_DIR_SUFFIX) ?: return
        val published = animeDirectory.findFile(finalDirectoryName)
        if (published != null && isEpisodePackageValid(published)) {
            backup.delete()
            return
        }
        published?.delete()
        backup.renameTo(finalDirectoryName)
    }

    private fun renameDirectory(parent: UniFile, oldName: String, newName: String): Boolean {
        val directory = parent.findFile(oldName) ?: return false
        if (directory.name == newName) return true
        if (parent.findFile(newName) != null && !oldName.equals(newName, ignoreCase = true)) return false
        if (oldName.equals(newName, ignoreCase = true)) {
            val stagingName = newName + AnimeDownloadManager.TMP_DIR_SUFFIX
            if (!directory.renameTo(stagingName)) return false
            if (directory.renameTo(newName)) return true
            directory.renameTo(oldName)
            return false
        }
        return directory.renameTo(newName)
    }

    companion object {
        const val BACKUP_DIR_SUFFIX = ".animebak"
        const val MAX_MANIFEST_BYTES = 1024L * 1024L
        val LEGACY_VIDEO_EXTENSIONS = listOf(".mp4", ".mkv", ".webm", ".m3u8", ".m3u")
    }
}

private fun String.isSafeArtifactName(): Boolean =
    isNotBlank() && this != "." && this != ".." && '/' !in this && '\\' !in this

private fun InputStream.readBoundedText(maxBytes: Long): String {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "Anime download manifest is too large" }
        output.write(buffer, 0, read)
    }
    return output.toString(Charsets.UTF_8.name())
}
