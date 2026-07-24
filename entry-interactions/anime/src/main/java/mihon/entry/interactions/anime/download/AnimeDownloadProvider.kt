package mihon.entry.interactions.anime.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.util.lang.Hash.md5
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.serialization.json.Json
import logcat.LogPriority
import mihon.entry.interactions.anime.download.model.AnimeDownloadManifest
import mihon.entry.interactions.anime.download.model.DownloadedArtifact
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.displayablePath
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException
import java.io.InputStream

internal class AnimeDownloadProvider(
    private val context: Context,
    private val storageManager: StorageManager = Injekt.get(),
    private val libraryPreferences: GlobalLibraryPreferences = Injekt.get(),
    private val json: Json = Injekt.get(),
) {

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

        val sourceDirName = getSourceDirName(source)
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

    fun findSourceDir(source: UnifiedSource): UniFile? {
        return downloadsDir?.findFile(getSourceDirName(source))
    }

    fun findAnimeDir(animeTitle: String, source: UnifiedSource): UniFile? {
        return findSourceDir(source)?.findFile(getAnimeDirName(animeTitle))
    }

    fun findEpisodeDir(
        episodeName: String,
        episodeUrl: String,
        animeTitle: String,
        source: UnifiedSource,
    ): UniFile? {
        val animeDir = findAnimeDir(animeTitle, source)
        return getValidEpisodeDirNames(episodeName, episodeUrl).asSequence()
            .mapNotNull { animeDir?.findFile(it) }
            .filter(::isEpisodePackageValid)
            .firstOrNull()
    }

    fun findEpisodeDirs(
        episodes: List<EntryChapter>,
        anime: Entry,
        source: UnifiedSource,
    ): Pair<UniFile?, List<UniFile>> {
        val animeDir = findAnimeDir(anime.title, source) ?: return null to emptyList()
        return animeDir to episodes.mapNotNull { episode ->
            getValidEpisodeDirNames(episode.name, episode.url).asSequence()
                .mapNotNull { animeDir.findFile(it) }
                .firstOrNull()
        }
    }

    fun getSourceDirName(source: UnifiedSource): String {
        return getSourceDirName(source.name)
    }

    fun getSourceDirName(sourceName: String): String {
        return DiskUtil.buildValidFilename(
            sourceName + " [anime]",
            disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get(),
        )
    }

    fun getAnimeDirName(animeTitle: String): String {
        return DiskUtil.buildValidFilename(
            animeTitle,
            disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get(),
        )
    }

    fun getEpisodeDirName(
        episodeName: String,
        episodeUrl: String,
        disallowNonAsciiFilenames: Boolean = libraryPreferences.disallowNonAsciiFilenames.get(),
    ): String {
        var dirName = sanitizeEpisodeName(episodeName)
        dirName = DiskUtil.buildValidFilename(dirName, DiskUtil.MAX_FILE_NAME_BYTES - 8, disallowNonAsciiFilenames)
        dirName += "_" + md5(episodeUrl).take(6)
        return dirName
    }

    private fun getLegacyEpisodeDirNames(
        episodeName: String,
        episodeUrl: String,
    ): List<String> {
        val episodeNameV1 = DiskUtil.buildValidFilename(sanitizeEpisodeName(episodeName))
        val otherEpisodeDirName = getEpisodeDirName(
            episodeName = episodeName,
            episodeUrl = episodeUrl,
            disallowNonAsciiFilenames = !libraryPreferences.disallowNonAsciiFilenames.get(),
        )

        return buildList(2) {
            add(episodeNameV1)
            add(otherEpisodeDirName)
        }
    }

    private fun sanitizeEpisodeName(episodeName: String): String {
        return episodeName.ifBlank { "Episode" }
    }

    fun getValidEpisodeDirNames(episodeName: String, episodeUrl: String): List<String> {
        val episodeDirName = getEpisodeDirName(episodeName, episodeUrl)
        val legacyEpisodeDirNames = getLegacyEpisodeDirNames(episodeName, episodeUrl)
        return buildList {
            add(episodeDirName)
            legacyEpisodeDirNames.forEach(::add)
        }
    }

    fun createArtifactRecords(directory: UniFile): List<DownloadedArtifact> {
        return directory.listFiles().orEmpty()
            .filter { it.isFile && it.name != AnimeDownloader.MANIFEST_FILE_NAME }
            .map { file ->
                DownloadedArtifact(
                    fileName = checkNotNull(file.name),
                    storedSize = file.length(),
                )
            }
    }

    fun readValidManifest(directory: UniFile): AnimeDownloadManifest? = runCatching {
        val manifestFile = directory.findFile(AnimeDownloader.MANIFEST_FILE_NAME) ?: return null
        require(manifestFile.isFile && manifestFile.length() != 0L)
        val manifest = manifestFile.openInputStream().use {
            json.decodeFromString<AnimeDownloadManifest>(it.readBoundedText(MAX_MANIFEST_BYTES))
        }
        requireReferencedArtifact(directory, manifest.video.fileName)
        manifest.subtitles.forEach { requireReferencedArtifact(directory, it.fileName) }
        val referencedNames = buildSet {
            add(manifest.video.fileName)
            manifest.subtitles.mapTo(this) { it.fileName }
        }
        val artifactNames = manifest.artifacts.map { it.fileName }
        require(artifactNames.distinct().size == artifactNames.size)
        if (artifactNames.isNotEmpty()) require(artifactNames.containsAll(referencedNames))
        manifest.artifacts.forEach { artifact ->
            val file = requireReferencedArtifact(directory, artifact.fileName)
            require(file.length() == artifact.storedSize)
        }
        manifest
    }.getOrNull()

    fun isEpisodePackageValid(directory: UniFile): Boolean {
        if (!directory.isDirectory) return false
        if (directory.findFile(AnimeDownloader.MANIFEST_FILE_NAME) != null) {
            return readValidManifest(directory) != null
        }
        return directory.listFiles().orEmpty().any { file ->
            file.isFile && LEGACY_VIDEO_EXTENSIONS.any { extension ->
                file.name?.endsWith(extension, ignoreCase = true) == true
            }
        }
    }

    fun publishEpisodePackage(
        animeDirectory: UniFile,
        stagingDirectory: UniFile,
        finalDirectoryName: String,
    ): Result<UniFile> = runCatching {
        require(readValidManifest(stagingDirectory) != null) { "Anime staging package failed validation" }
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
        if (published == null || readValidManifest(published) == null) {
            published?.delete()
            animeDirectory.findFile(backupName)?.renameTo(finalDirectoryName)
            throw IOException("Published anime download failed validation")
        }
        animeDirectory.findFile(backupName)?.delete()
        published
    }

    fun beginEpisodePackage(animeDirectory: UniFile, finalDirectoryName: String): Result<UniFile> = runCatching {
        recoverEpisodePackage(animeDirectory, finalDirectoryName)
        val stagingName = finalDirectoryName + AnimeDownloadManager.TMP_DIR_SUFFIX
        animeDirectory.findFile(stagingName)?.delete()
        animeDirectory.createDirectory(stagingName)
            ?: throw IOException("Unable to create temporary anime download directory")
    }

    fun recoverEpisodePackages(animeDirectory: UniFile) {
        animeDirectory.listFiles().orEmpty()
            .mapNotNull { it.name }
            .filter { it.endsWith(BACKUP_DIR_SUFFIX) }
            .map { it.removeSuffix(BACKUP_DIR_SUFFIX) }
            .forEach { recoverEpisodePackage(animeDirectory, it) }
    }

    fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource): Boolean {
        val root = downloadsDir ?: return false
        return renameDirectory(root, getSourceDirName(oldSource), getSourceDirName(newSource))
    }

    fun renameEntry(source: UnifiedSource, entry: Entry, newTitle: String): Boolean {
        val sourceDirectory = findSourceDir(source) ?: return false
        return renameDirectory(sourceDirectory, getAnimeDirName(entry.title), getAnimeDirName(newTitle))
    }

    private fun requireReferencedArtifact(directory: UniFile, fileName: String): UniFile {
        require(fileName.isSafeArtifactName())
        return directory.findFile(fileName)
            ?.takeIf { it.isFile && it.length() != 0L }
            ?: throw IOException("Downloaded anime artifact $fileName is missing or empty")
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
