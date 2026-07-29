package mihon.entry.interactions.book.prose.continuous

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mihon.entry.interactions.book.document.model.BookDocumentPosition
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.prose.prepareHtmlBookDocument
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ContinuousProseDocumentProjectionTest {
    @Test
    fun `projection keeps semantic reading order without structural paragraph terminators`() {
        val section = section(
            """
                <h2>Heading</h2>
                <p>First paragraph.</p>
                <p>Second paragraph with <a href="https://example.test/">a link</a>.</p>
                <ol start="3"><li>Third</li><li>Fourth</li></ol>
                <table><caption>Values</caption><tr><th>Name</th><td>Katari</td></tr></table>
                <details><summary>More</summary><p>Hidden text</p></details>
            """.trimIndent(),
        )

        val projection = buildContinuousProseProjection(
            generation = 7,
            window = EntryChildWindow(section.owner, null, null),
            loaded = mapOf(section.owner.id to section),
        )
        val root = Json.parseToJsonElement(projection.json).jsonObject
        val current = root["items"]!!.jsonArray
            .map { it.jsonObject }
            .single { it["type"]!!.jsonPrimitive.content == "section" }
        val blocks = current["blocks"]!!.jsonArray.map { it.jsonObject }

        assertEquals(
            listOf("heading", "paragraph", "paragraph", "list", "table", "disclosure"),
            blocks.map { it["role"]!!.jsonPrimitive.content },
        )
        val paragraphText = blocks[1]["content"]!!.jsonObject["text"]!!.jsonPrimitive.content
        assertEquals("First paragraph.", paragraphText)
        assertFalse(paragraphText.endsWith('\n'))
        assertEquals(
            "list",
            blocks[3]["content"]!!.jsonObject["kind"]!!.jsonPrimitive.content,
        )
        assertEquals(
            listOf("Third", "Fourth"),
            blocks[3]["content"]!!.jsonObject["items"]!!.jsonArray.map {
                it.jsonObject["text"]!!.jsonPrimitive.content
            },
        )
        assertEquals(
            "Hidden text",
            blocks[5]["content"]!!.jsonObject["body"]!!.jsonArray
                .single()
                .jsonObject["content"]!!
                .jsonObject["text"]!!
                .jsonPrimitive.content,
        )
    }

    @Test
    fun `message validation rejects stale unknown and out of range document positions`() {
        val projection = ContinuousProseProjection(
            generation = 9,
            currentSectionKey = "chapter",
            json = "{}",
            resources = emptyMap(),
            blockLengths = mapOf("chapter" to mapOf("paragraph" to 12)),
        )
        val validator = ContinuousProseMessageValidator()

        assertNull(
            validator.parse(
                """{"type":"position","generation":8,"sectionKey":"chapter","blockId":"paragraph","offset":4,"progression":0.3}""",
                projection,
            ),
        )
        assertNull(
            validator.parse(
                """{"type":"position","generation":9,"sectionKey":"other","blockId":"paragraph","offset":4,"progression":0.3}""",
                projection,
            ),
        )
        assertNull(
            validator.parse(
                """{"type":"position","generation":9,"sectionKey":"chapter","blockId":"paragraph","offset":13,"progression":0.3}""",
                projection,
            ),
        )
        val event = validator.parse(
            """{"type":"position","generation":9,"sectionKey":"chapter","blockId":"paragraph","offset":12,"progression":1.0}""",
            projection,
        )
        assertIs<ContinuousProseEvent.Position>(event)
        assertEquals(BookDocumentPosition(event.position.blockId, 12), event.position)
    }

    @Test
    fun `message validation accepts only bounded selection geometry and web links`() {
        val projection = ContinuousProseProjection(
            generation = 4,
            currentSectionKey = "chapter",
            json = "{}",
            resources = emptyMap(),
            blockLengths = mapOf("chapter" to mapOf("paragraph" to 12)),
        )
        val validator = ContinuousProseMessageValidator()

        assertNull(
            validator.parse(
                """{"type":"external-link","generation":4,"url":"javascript:alert(1)"}""",
                projection,
            ),
        )
        assertNull(
            validator.parse(
                """{"type":"selection","generation":4,"identity":"x","text":"Text","left":2,"top":4,"right":1,"bottom":8}""",
                projection,
            ),
        )
        val event = validator.parse(
            """{"type":"selection","generation":4,"identity":"x","text":"First\nSecond","left":1,"top":2,"right":30,"bottom":40}""",
            projection,
        )
        assertIs<ContinuousProseEvent.Selection>(event)
        assertEquals("First\nSecond", event.text)
        assertTrue(event.boundsInWebView.width() > 0)
    }

    private fun section(html: String): BookDocumentSection<EntryChapter> {
        val chapter = EntryChapter.create().copy(id = 1L, entryId = 2L, name = "Chapter 1")
        val document = prepareHtmlBookDocument("chapter-1", "r1", html)
        return BookDocumentSection(
            key = chapter.id.toString(),
            owner = chapter,
            document = document,
            initialPosition = document.document.positionAtProgression(0f),
            resourceLoader = null,
        )
    }
}
