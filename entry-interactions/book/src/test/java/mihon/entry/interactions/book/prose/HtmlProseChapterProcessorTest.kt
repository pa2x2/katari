package mihon.entry.interactions.book.prose

import android.text.style.URLSpan
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookContentResource
import mihon.book.api.BookContentResourceGroup
import mihon.book.api.BookContentResourcePage
import mihon.book.api.BookFailureReason
import mihon.book.api.BookLocator
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCapability
import mihon.entry.interactions.book.BookByteRange
import mihon.entry.interactions.book.BookContentSession
import mihon.entry.interactions.book.BookOpenResult
import mihon.entry.interactions.book.MaterializedBookResource
import mihon.entry.interactions.book.OpenedBookResource
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentBorderStyle
import mihon.entry.interactions.book.document.model.BookDocumentFontFamily
import mihon.entry.interactions.book.document.resource.PROSE_FONT_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class HtmlProseChapterProcessorTest {
    private val processor = HtmlProseChapterProcessor()

    @Test
    fun `supports only unprotected prose chapter html`() {
        assertTrue(processor.supports(BookContentDescriptor("text/html", profile = "prose-chapter")))
        assertFalse(processor.supports(BookContentDescriptor("text/html")))
        assertFalse(processor.supports(BookContentDescriptor("text/html", profile = "web-novel")))
        assertFalse(
            processor.supports(
                BookContentDescriptor("text/html", profile = "prose-chapter", protection = "vendor-drm"),
            ),
        )
    }

    @Test
    fun `opens one selected chapter and preserves only passive safe links`() = runTest {
        val content = TestProseContentSession(
            html = """
                <h1>Chapter 7</h1>
                <p onclick="steal()">Hello <em>reader</em>.</p>
                <script>steal()</script>
                <a href="https://example.com">external</a>
                <a href="#note">note</a>
                <aside id="note">Footnote</aside>
                <img src="https://example.com/tracker.png">
            """.trimIndent(),
        )

        val result = assertIs<BookOpenResult.Success>(processor.open(content))
        val session = assertIs<HtmlProseChapterSession>(result.session)
        val text = session.document.combinedText
        val urls = text.getSpans(0, text.length, URLSpan::class.java).map(URLSpan::getURL)

        assertEquals(listOf("chapter-7"), session.publication.readingOrder.map { it.id })
        assertTrue(text.contains("Hello reader."))
        assertTrue(text.contains("Footnote"))
        assertFalse(text.contains("steal"))
        assertEquals(listOf("https://example.com", "#note"), urls)
        assertTrue("note" in session.document.document.anchors)
        assertEquals(1, content.openCount)
    }

    @Test
    fun `unsupported content blocks become one visible marker per outer block`() = runTest {
        val content = TestProseContentSession(
            html = """
                <p>Before</p>
                <audio><source src="audio.mp3"></audio>
                <video><source src="video.mp4"><track src="captions.vtt"></video>
                <iframe src="https://example.invalid/embed"></iframe>
                <object data="object.bin"><embed src="nested.bin"></object>
                <embed src="standalone.bin">
                <canvas>fallback</canvas>
                <svg><circle></circle></svg>
                <div class="math-equation" data-format="latex">equation</div>
                <div class="specialized-chart" data-chart-format="v1"><canvas>nested</canvas></div>
                <script>active()</script>
                <form><input value="tracking"></form>
                <div class="advertisement">Ad</div>
                <p>A <provider-inline>harmless wrapper</provider-inline> remains.</p>
            """.trimIndent(),
        )

        val result = assertIs<BookOpenResult.Success>(processor.open(content))
        val session = assertIs<HtmlProseChapterSession>(result.session)
        val unsupported = session.document.document.blocks.filter {
            it.role.kind == BookDocumentBlockKind.UNSUPPORTED
        }

        assertEquals(9, unsupported.size)
        assertTrue(unsupported.all { it.plainText == UNSUPPORTED_CONTENT_BLOCK_TEXT })
        assertTrue(unsupported.all { it.content is BookDocumentBlockContent.Unsupported })
        assertEquals(9, session.document.combinedText.toString().windowedCount(UNSUPPORTED_CONTENT_BLOCK_TEXT))
        assertTrue(session.document.combinedText.contains("harmless wrapper"))
        assertFalse(session.document.combinedText.contains("active"))
        assertFalse(session.document.combinedText.contains("tracking"))
        assertFalse(session.document.combinedText.contains("Ad"))
        assertTrue(session.requiredResourceIds.isEmpty())
        assertEquals(1, content.openCount)
    }

    @Test
    fun `safe styles and catalogued assets survive sanitization and use controlled loading`() = runTest {
        val imageBytes = byteArrayOf(1, 2, 3, 4)
        val fontBytes = byteArrayOf(5, 6, 7)
        val inlineFontBytes = byteArrayOf(8, 9, 10)
        val assetReadThread = AtomicReference<Thread>()
        val callerThread = Thread.currentThread()
        val content = TestProseContentSession(
            html = """
                <style>
                    @font-face { font-family: Fixture; src: url("fixture-font"); }
                    @font-face { font-family: InlineFixture; src: url("inline-font"); }
                    .panel {
                        color: white;
                        background: #123456;
                        border: 2px dashed red;
                        padding: 1rem;
                        text-align: center;
                        font: 125% Fixture;
                        font-family: Fixture;
                        font-size: 125%;
                    }
                    .spoiler {
                        color: #111111;
                        background: #111111;
                        font-family: InlineFixture;
                        font-size: 90%;
                    }
                </style>
                <div class="panel">Styled status</div>
                <p>Visible <span class="spoiler">inline secret</span> after.</p>
                <figure>
                    <img src="fixture-image" alt="A readable seal" width="640" height="320">
                    <figcaption>Seal caption</figcaption>
                </figure>
                <img src="http://example.invalid/insecure.png" alt="Rejected insecure image">
            """.trimIndent(),
            subordinateResources = mapOf(
                "fixture-image" to TestResource("image/png", imageBytes, assetReadThread::set),
                "fixture-font" to TestResource("font/ttf", fontBytes),
                "inline-font" to TestResource("font/ttf", inlineFontBytes),
            ),
        )

        val session = assertIs<HtmlProseChapterSession>(
            assertIs<BookOpenResult.Success>(processor.open(content)).session,
        )
        val styled = session.document.document.blocks.single { it.plainText == "Styled status" }
        val figure = assertIs<BookDocumentBlockContent.Figure>(
            session.document.document.blocks.single {
                it.content is BookDocumentBlockContent.Figure
            }.content,
        )

        assertEquals(BookDocumentBorderStyle.DASHED, styled.style.border?.style)
        assertEquals(BookDocumentBlockKind.CALLOUT, styled.role.kind)
        assertEquals(1f, styled.style.paddingEm)
        assertEquals(1.25f, styled.style.fontSizeScale)
        assertIs<BookDocumentFontFamily.Resource>(styled.style.fontFamily)
        val inlineStyled = session.document.document.blocks.single { it.plainText.contains("inline secret") }
        val inlineRange = inlineStyled.inlineStyles.single()
        assertEquals("inline secret", inlineStyled.plainText.substring(inlineRange.start, inlineRange.endExclusive))
        assertEquals(0xFF111111, inlineRange.style.foregroundArgb)
        assertEquals(0xFF111111, inlineRange.style.backgroundArgb)
        assertEquals(0.9f, inlineRange.style.fontSizeScale)
        assertEquals(BookDocumentFontFamily.Resource("inline-font"), inlineRange.style.fontFamily)
        assertEquals("A readable seal", figure.image.alternativeText)
        assertEquals("Seal caption", figure.caption)
        assertEquals(setOf("fixture-image", "fixture-font", "inline-font"), session.requiredResourceIds)
        assertEquals(
            mapOf(
                "fixture-image" to PROSE_IMAGE_RESOURCE_REQUIREMENT,
                "fixture-font" to PROSE_FONT_RESOURCE_REQUIREMENT,
                "inline-font" to PROSE_FONT_RESOURCE_REQUIREMENT,
            ),
            session.resourceRequirements,
        )
        assertEquals(
            imageBytes.toList(),
            session.resourceLoader.load("fixture-image", setOf("image/png"), 16).getOrThrow().bytes.toList(),
        )
        assertNotEquals(callerThread, assetReadThread.get())
        assertTrue(session.resourceLoader.load("fixture-image", setOf("image/png"), 16).isSuccess)
        assertTrue(session.resourceLoader.load("fixture-image", setOf("font/ttf"), 16).isFailure)
        assertTrue(session.resourceLoader.load("fixture-image", setOf("image/png"), 2).isFailure)
        assertEquals(2, content.openCount)
    }

    @Test
    fun `migration maps portable progression onto the target prose resource`() = runTest {
        val result = assertIs<BookOpenResult.Success>(
            processor.open(TestProseContentSession(html = "<p>Target chapter</p>")),
        )
        val session = assertIs<HtmlProseChapterSession>(result.session)

        val reconciled = session.reconcileMigratedLocator(
            BookLocator(
                resourceId = "source-chapter",
                progression = 0.4,
                totalProgression = 0.4,
                fragments = listOf("source-anchor"),
            ),
        )

        assertEquals(
            BookLocator(
                resourceId = "chapter-7",
                progression = 0.4,
                totalProgression = 0.4,
            ),
            reconciled,
        )
    }

    @Test
    fun `rejects a sibling catalog instead of constructing a multi chapter publication`() = runTest {
        val content = TestProseContentSession(
            html = "<p>Selected chapter</p>",
            primaryResourceIds = listOf("chapter-7", "chapter-8"),
        )

        val result = assertIs<BookOpenResult.Failure>(processor.open(content))

        assertEquals(BookFailureReason.CONTENT_UNAVAILABLE, result.failure.reason)
        assertEquals(0, content.openCount)
    }

    @Test
    fun `rejects a chapter with no readable prose after sanitizing`() = runTest {
        val content = TestProseContentSession(html = "<script>onlyActiveContent()</script>")

        val result = assertIs<BookOpenResult.Failure>(processor.open(content))

        assertEquals(BookFailureReason.MALFORMED_CONTENT, result.failure.reason)
    }

    @Test
    fun `does not open purchase required chapter content`() = runTest {
        val content = TestProseContentSession(
            html = "<p>Preview must not be rendered as the chapter.</p>",
            availability = BookResourceAvailability.PURCHASE_REQUIRED,
        )

        val result = assertIs<BookOpenResult.Failure>(processor.open(content))

        assertEquals(BookFailureReason.CONTENT_UNAVAILABLE, result.failure.reason)
        assertEquals(0, content.openCount)
    }
}

private fun String.windowedCount(value: String): Int {
    var count = 0
    var offset = 0
    while (true) {
        val next = indexOf(value, offset)
        if (next < 0) return count
        count++
        offset = next + value.length
    }
}

private class TestProseContentSession(
    private val html: String,
    availability: BookResourceAvailability = BookResourceAvailability.AVAILABLE,
    override val primaryResourceIds: List<String> = listOf("chapter-7"),
    private val subordinateResources: Map<String, TestResource> = emptyMap(),
) : BookContentSession {
    override val descriptor = BookContentDescriptor("text/html", profile = "prose-chapter")
    override val publicationId = "source:novel"
    override val revision = "unversioned"
    override val catalogRevision: String? = null
    override val catalogCoverage = BookCatalogCoverage.PARTIAL
    override val resourceHierarchy = emptyList<BookContentResourceGroup>()
    private val primaryResource = BookContentResource(
        id = "chapter-7",
        title = "Chapter 7",
        mediaType = "text/html",
        size = html.encodeToByteArray().size.toLong(),
        availability = availability,
        capabilities = setOf(BookResourceCapability.STREAM),
    )
    var openCount = 0
        private set

    override suspend fun listResources(cursor: String?, limit: Int): Result<BookContentResourcePage> =
        Result.success(
            BookContentResourcePage(
                listOf(primaryResource) + subordinateResources.map { (id, resource) ->
                    BookContentResource(
                        id = id,
                        mediaType = resource.mediaType,
                        size = resource.bytes.size.toLong(),
                        availability = BookResourceAvailability.AVAILABLE,
                        capabilities = setOf(BookResourceCapability.STREAM),
                    )
                },
            ),
        )

    override suspend fun getResource(resourceId: String): Result<BookContentResource> =
        when {
            resourceId == primaryResource.id -> Result.success(primaryResource)
            resourceId in subordinateResources -> {
                val resource = subordinateResources.getValue(resourceId)
                Result.success(
                    BookContentResource(
                        id = resourceId,
                        mediaType = resource.mediaType,
                        size = resource.bytes.size.toLong(),
                        availability = BookResourceAvailability.AVAILABLE,
                        capabilities = setOf(BookResourceCapability.STREAM),
                    ),
                )
            }
            else -> Result.failure(NoSuchElementException(resourceId))
        }

    override suspend fun openResource(resourceId: String, range: BookByteRange?): Result<OpenedBookResource> {
        val bytes = when {
            resourceId == primaryResource.id -> html.encodeToByteArray()
            else -> subordinateResources[resourceId]?.bytes
                ?: return Result.failure(NoSuchElementException(resourceId))
        }
        val metadata = getResource(resourceId).getOrThrow()
        openCount++
        return Result.success(
            object : OpenedBookResource {
                override val metadata = metadata
                override val stream = object : ByteArrayInputStream(bytes) {
                    override fun read(): Int {
                        subordinateResources[resourceId]?.onRead?.invoke(Thread.currentThread())
                        return super.read()
                    }

                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                        subordinateResources[resourceId]?.onRead?.invoke(Thread.currentThread())
                        return super.read(buffer, offset, length)
                    }
                }
                override fun close() = stream.close()
            },
        )
    }

    override suspend fun materializeResource(resourceId: String): Result<MaterializedBookResource> =
        Result.failure(UnsupportedOperationException("Prose chapters are streamed"))

    override fun close() = Unit
}

private data class TestResource(
    val mediaType: String,
    val bytes: ByteArray,
    val onRead: ((Thread) -> Unit)? = null,
)
