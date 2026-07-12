package mihon.entry.interactions.anime.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.util.lang.Hash.md5
import eu.kanade.tachiyomi.util.storage.DiskUtil
import logcat.LogPriority
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

internal class AnimeDownloadProvider(
    private val context: Context,
    private val storageManager: StorageManager = Injekt.get(),
    private val libraryPreferences: GlobalLibraryPreferences = Injekt.get(),
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
        return DiskUtil.buildValidFilename(
            source.name + " [anime]",
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
}
