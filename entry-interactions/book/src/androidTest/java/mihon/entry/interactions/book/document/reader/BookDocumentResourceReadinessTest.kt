package mihon.entry.interactions.book.document.reader

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderPalette
import mihon.entry.interactions.book.document.resource.BookPublicationResourceGateway
import mihon.entry.interactions.book.format.html.prosechapter.parsing.HtmlProseDocumentParser
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizationPolicy
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import mihon.entry.interactions.book.preparation.BookPublicationResource
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import mihon.entry.interactions.book.preparation.BookRemoteResourceReference
import mihon.entry.interactions.book.preparation.BookRemoteResourceType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class BookDocumentResourceReadinessTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun approving_resources_replaces_an_already_composed_image_failure_without_a_manual_retry() {
        val requests = AtomicInteger()
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val image = ByteArrayOutputStream().apply {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
        }.toByteArray()
        bitmap.recycle()
        val directory = Files.createTempDirectory(composeRule.activity.cacheDir.toPath(), "resource-readiness").toFile()
        val gateway = BookPublicationResourceGateway(
            packaged = object : BookPublicationResourceLoader {
                override suspend fun load(
                    resourceId: String,
                    acceptedMediaTypes: Set<String>,
                    maxBytes: Int,
                ): Result<BookPublicationResource> =
                    Result.failure(IllegalArgumentException("Unknown packaged resource"))
            },
            references = listOf(
                BookRemoteResourceReference("image", "https://assets.example/image.png", BookRemoteResourceType.IMAGE),
            ),
            httpClient = OkHttpClient.Builder().addInterceptor { chain ->
                requests.incrementAndGet()
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body(image.toResponseBody("image/png".toMediaType())).build()
            }.build(),
            snapshotDirectory = directory,
        )
        val document = HtmlProseDocumentParser().parse(
            "page",
            null,
            HtmlProseSanitizer.sanitize(
                "<img src='image' alt='Publication illustration'/>".encodeToByteArray(),
                HtmlProseSanitizationPolicy(resolveImageResource = { "image" }),
            ),
        )
        val block = document.blocks.single()
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalBookDocumentReaderPalette provides bookDocumentReaderPalette(BookDocumentReaderThemeMode.APP),
                    LocalBookDocumentSelectionChapterId provides 1L,
                ) {
                    BookDocumentFigureRenderer(
                        block.content as BookDocumentBlockContent.Figure,
                        block,
                        "image",
                        gateway,
                        {},
                        {},
                        {},
                    )
                }
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Publication illustration").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, requests.get())
        composeRule.runOnIdle { gateway.authorizeRemoteOrigins(setOf("https://assets.example:443")) }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription("Publication illustration").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Publication illustration").assertIsDisplayed()
        assertEquals(1, requests.get())
        directory.deleteRecursively()
    }
}
