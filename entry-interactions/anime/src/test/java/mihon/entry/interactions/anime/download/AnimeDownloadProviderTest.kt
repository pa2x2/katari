package mihon.entry.interactions.anime.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mihon.entry.interactions.anime.download.model.AnimeDownloadManifest
import mihon.entry.interactions.anime.download.model.DownloadedArtifact
import mihon.entry.interactions.anime.download.model.DownloadedVideo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.storage.service.StorageManager
import java.io.ByteArrayInputStream
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
class AnimeDownloadProviderTest {

    @Test
    fun `validates every recorded artifact before exposing a package`() {
        val videoBytes = "downloaded video".encodeToByteArray()
        val manifest = manifest(
            artifacts = listOf(
                DownloadedArtifact("video.mp4", videoBytes.size.toLong()),
            ),
        )
        val directory = packageDirectory(manifest, videoBytes)

        provider().readValidManifest(directory) shouldBe manifest
    }

    @Test
    fun `rejects a package whose recorded artifact was changed`() {
        val originalBytes = "downloaded video".encodeToByteArray()
        val manifest = manifest(
            artifacts = listOf(
                DownloadedArtifact("video.mp4", originalBytes.size.toLong()),
            ),
        )
        val changedBytes = "truncated".encodeToByteArray()
        val directory = packageDirectory(manifest, changedBytes)

        provider().readValidManifest(directory).shouldBeNull()
    }

    @Test
    fun `keeps legacy video packages discoverable`() {
        val legacyVideo = file("episode.MKV", "legacy".encodeToByteArray())
        val directory = mockk<UniFile> {
            every { isDirectory } returns true
            every { findFile(AnimeDownloader.MANIFEST_FILE_NAME) } returns null
            every { listFiles() } returns arrayOf(legacyVideo)
        }

        provider().isEpisodePackageValid(directory) shouldBe true
    }

    @Test
    fun `source and entry renames keep anime packages discoverable`() {
        val root = Files.createTempDirectory("katari-anime-downloads").toFile()
        val rootDirectory = UniFile.fromFile(root)!!
        val provider = provider(rootDirectory)
        val oldSource = source(42L, "Old Source")
        val newSource = source(42L, "New Source")
        val entry = Entry.create().copy(
            id = 1L,
            source = oldSource.id,
            title = "Old Anime",
            type = EntryType.ANIME,
        )
        val sourceDirectory = rootDirectory.createDirectory(provider.getSourceDirName(oldSource))!!
        sourceDirectory.createDirectory(provider.getAnimeDirName(entry.title))!!

        provider.renameSource(oldSource, newSource) shouldBe true
        provider.renameEntry(newSource, entry, "New Anime") shouldBe true

        rootDirectory.findFile(provider.getSourceDirName(oldSource)) shouldBe null
        val renamedSource = rootDirectory.findFile(provider.getSourceDirName(newSource))!!
        renamedSource.findFile(provider.getAnimeDirName(entry.title)) shouldBe null
        renamedSource.findFile(provider.getAnimeDirName("New Anime"))?.isDirectory shouldBe true
    }

    @Test
    fun `reindex recovery restores a package preserved during interrupted publication`() {
        val root = UniFile.fromFile(Files.createTempDirectory("katari-anime-recovery").toFile())!!
        val animeDirectory = root.createDirectory("Anime")!!
        val completed = animeDirectory.createDirectory("Episode")!!
        completed.createFile("video.mp4")!!.openOutputStream().use {
            it.write("offline video".encodeToByteArray())
        }
        completed.renameTo("Episode" + AnimeDownloadProvider.BACKUP_DIR_SUFFIX) shouldBe true

        provider(root).recoverEpisodePackages(animeDirectory)

        val recovered = animeDirectory.findFile("Episode")!!
        provider(root).isEpisodePackageValid(recovered) shouldBe true
        animeDirectory.findFile("Episode" + AnimeDownloadProvider.BACKUP_DIR_SUFFIX) shouldBe null
    }

    private fun provider(): AnimeDownloadProvider {
        return provider(downloadsDirectory = null)
    }

    private fun provider(downloadsDirectory: UniFile?): AnimeDownloadProvider {
        val preferences = mockk<GlobalLibraryPreferences> {
            every { disallowNonAsciiFilenames.get() } returns false
        }
        val storageManager = mockk<StorageManager> {
            every { getDownloadsDirectory() } returns downloadsDirectory
        }
        return AnimeDownloadProvider(
            context = mockk<Context>(relaxed = true),
            storageManager = storageManager,
            libraryPreferences = preferences,
            json = json,
        )
    }

    private fun source(sourceId: Long, sourceName: String): UnifiedSource = mockk {
        every { id } returns sourceId
        every { name } returns sourceName
    }

    private fun packageDirectory(manifest: AnimeDownloadManifest, videoBytes: ByteArray): UniFile {
        val manifestBytes = json.encodeToString(manifest).encodeToByteArray()
        val manifestFile = file(AnimeDownloader.MANIFEST_FILE_NAME, manifestBytes)
        val videoFile = file("video.mp4", videoBytes)
        return mockk {
            every { isDirectory } returns true
            every { findFile(AnimeDownloader.MANIFEST_FILE_NAME) } returns manifestFile
            every { findFile("video.mp4") } returns videoFile
        }
    }

    private fun file(name: String, bytes: ByteArray): UniFile = mockk {
        every { this@mockk.name } returns name
        every { isFile } returns true
        every { length() } returns bytes.size.toLong()
        every { openInputStream() } answers { ByteArrayInputStream(bytes) }
    }

    private fun manifest(artifacts: List<DownloadedArtifact>) = AnimeDownloadManifest(
        animeId = 1L,
        episodeId = 2L,
        animeTitle = "Anime",
        episodeTitle = "Episode",
        originalEpisodeUrl = "/episode/2",
        qualityMode = "BALANCED",
        selection = PlaybackSelection(streamKey = "stream"),
        video = DownloadedVideo(
            fileName = "video.mp4",
            sourceUrl = "https://example.com/video.mp4",
            headers = emptyMap(),
            label = "Video",
            streamType = VideoStreamType.PROGRESSIVE,
            mimeType = "video/mp4",
        ),
        subtitles = emptyList(),
        artifacts = artifacts,
    )

    private companion object {
        val json = Json { encodeDefaults = true }
    }
}
