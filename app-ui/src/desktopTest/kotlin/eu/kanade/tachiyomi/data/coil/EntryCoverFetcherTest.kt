package eu.kanade.tachiyomi.data.coil

import app.katari.entry.interactions.source.EntryCoverNetworkResolution
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Path.Companion.toOkioPath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.core.common.storage.StorageFileResolver
import java.io.File

class EntryCoverFetcherTest {

    @TempDir
    lateinit var dir: File

    @Test
    fun `a web page served in place of a library cover is never kept, so the next load downloads it`() = runTest {
        val diskCache = DiskCache.Builder().directory(dir.resolve("disk").toOkioPath()).build()
        val imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE).diskCache { diskCache }.build()
        val coverFile = dir.resolve("covers/cover")
        var served = BROWSER_CHECK_PAGE to "text/html"
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(served.first.toResponseBody(served.second.toMediaType()))
                    .build()
            }
            .build()
        val fetcher = EntryCoverFetcher(
            url = "https://example.org/cover.jpg",
            isLibraryEntry = true,
            options = Options(PlatformContext.INSTANCE),
            coverFileLazy = lazy { coverFile },
            customCoverFileLazy = lazy { dir.resolve("custom") },
            diskCacheKeyLazy = lazy { "cover" },
            sourceNetworkResolutionLazy = lazy { EntryCoverNetworkResolution.Missing(1L) },
            callFactoryLazy = lazy { client },
            storageFiles = object : StorageFileResolver {
                override fun fromUri(uri: String) = null
            },
            imageLoader = imageLoader,
        )

        // Kept by a version that cached whatever the site answered
        coverFile.parentFile.mkdirs()
        coverFile.writeBytes(BROWSER_CHECK_PAGE)
        (fetcher.fetch() as SourceFetchResult).source.close()
        coverFile.exists() shouldBe false
        diskCache.openSnapshot("cover") shouldBe null

        served = PNG to "image/png"
        (fetcher.fetch() as SourceFetchResult).source.close()
        coverFile.readBytes().toList() shouldBe PNG.toList()
    }

    private companion object {
        val BROWSER_CHECK_PAGE = "<!doctype html><title>Browser verification</title>".toByteArray()
        val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D)
    }
}
