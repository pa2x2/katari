package mihon.entry.interactions.anime.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
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
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.storage.service.StorageManager
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
class AnimeDownloadProviderTest {

    @Test
    fun `exposes a package only while every recorded artifact is intact`() {
        val videoBytes = "downloaded video".encodeToByteArray()
        val manifest = manifest(
            artifacts = listOf(
                DownloadedArtifact("video.mp4", videoBytes.size.toLong()),
            ),
        )

        provider().readEpisodePackage(packageDirectory(manifest, videoBytes))?.manifest shouldBe manifest
        provider().readEpisodePackage(packageDirectory(manifest, "truncated".encodeToByteArray())).shouldBeNull()
    }

    @Test
    fun `a package downloaded before servers and external audio still opens`() {
        // As written by the release before them, field for field.
        val manifest = """
            {"animeId":1,"episodeId":2,"animeTitle":"Anime","episodeTitle":"Episode",
            "originalEpisodeUrl":"/episode/2","qualityMode":"BALANCED",
            "selection":{"dubKey":"ja","streamKey":"1080p","sourceQualityKey":null},
            "video":{"fileName":"video.mp4","sourceUrl":"https://example.com/video.mp4","headers":{},
            "label":"1080p","streamType":"PROGRESSIVE","mimeType":"video/mp4"},
            "subtitles":[],"artifacts":[{"fileName":"video.mp4","storedSize":5}]}
        """.trimIndent().encodeToByteArray()

        val read = provider().readEpisodePackage(packageDirectory(manifest, "video".encodeToByteArray()))?.manifest

        read?.selection shouldBe PlaybackSelection(dubKey = "ja", streamKey = "1080p")
        read?.audioTracks shouldBe emptyList()
    }

    @Test
    fun `keeps legacy video packages discoverable`() {
        val directory = temporaryDirectory()
        directory.write("episode.MKV", "legacy".encodeToByteArray())

        provider().isEpisodePackageValid(directory) shouldBe true
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

    private fun packageDirectory(manifest: AnimeDownloadManifest, videoBytes: ByteArray): UniFile {
        return packageDirectory(json.encodeToString(manifest).encodeToByteArray(), videoBytes)
    }

    private fun packageDirectory(manifestBytes: ByteArray, videoBytes: ByteArray): UniFile {
        return temporaryDirectory().apply {
            write(AnimeDownloader.MANIFEST_FILE_NAME, manifestBytes)
            write("video.mp4", videoBytes)
        }
    }

    private fun temporaryDirectory(): UniFile =
        UniFile.fromFile(Files.createTempDirectory("katari-anime-package").toFile())!!

    private fun UniFile.write(name: String, bytes: ByteArray) {
        createFile(name)!!.openOutputStream().use { it.write(bytes) }
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
