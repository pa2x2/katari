package mihon.entry.interactions.book.prose.continuous

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.prose.prepareHtmlBookDocument
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.domain.entry.model.EntryChapter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ContinuousProseWebViewTest {
    @Test
    fun selectionCrossesParagraphsWhileCssSpacingHasNoSelectableTerminalLine() {
        val chapter = EntryChapter.create().copy(id = 1L, entryId = 2L, name = "Chapter 1")
        val document = prepareHtmlBookDocument(
            resourceId = "chapter-1",
            revision = "r1",
            bodyHtml = "<p>First paragraph.</p><p>Second paragraph.</p>",
        )
        val section = BookDocumentSection(
            key = chapter.id.toString(),
            owner = chapter,
            document = document,
            initialPosition = document.document.positionAtProgression(0f),
            resourceLoader = null,
        )
        val projection = buildContinuousProseProjection(
            generation = 1,
            window = EntryChildWindow(chapter, null, null),
            loaded = mapOf(chapter.id to section),
        )
        val prepared = CountDownLatch(1)
        lateinit var controller: ContinuousProseWebViewController

        ActivityScenario.launch(ContinuousProseWebViewTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                controller = ContinuousProseWebViewController(activity)
                controller.callbacks = {
                    ContinuousProseWebViewCallbacks(
                        onEvent = { event, _ ->
                            if (event is ContinuousProseEvent.Prepared) prepared.countDown()
                        },
                        onFailure = { error -> throw AssertionError(error) },
                    )
                }
                activity.container.addView(
                    controller.webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                controller.update(
                    projection = projection,
                    sections = mapOf(section.key to section),
                    command = continuousProseRenderCommand(
                        projection,
                        ContinuousProseRenderSettings(
                            backgroundCss = "#ffffffff",
                            foregroundCss = "#202124ff",
                            fontFamilyCss = "serif",
                            fontSizePercent = 100,
                            lineHeightPercent = 160,
                            pageMarginsPercent = 100,
                            textAlignmentCss = "start",
                        ),
                        section.key,
                        section.initialPosition,
                    ),
                )
            }
            assertTrue("WebView projection did not become ready", prepared.await(10, TimeUnit.SECONDS))

            val result = evaluate(
                scenario,
                """
                    (() => {
                      const paragraphs = document.querySelectorAll('.section.current p.block');
                      const selection = getSelection();
                      const before = scrollY;
                      const wholeFirst = document.createRange();
                      wholeFirst.selectNodeContents(paragraphs[0]);
                      selection.removeAllRanges();
                      selection.addRange(wholeFirst);
                      const first = selection.toString();
                      const across = document.createRange();
                      across.setStart(paragraphs[0].firstChild, 0);
                      across.setEnd(paragraphs[1].firstChild, paragraphs[1].textContent.length);
                      selection.removeAllRanges();
                      selection.addRange(across);
                      return JSON.stringify({
                        first,
                        across: selection.toString(),
                        scrollChanged: scrollY !== before,
                        firstHasTerminalNewline: paragraphs[0].textContent.endsWith('\n')
                      });
                    })()
                """.trimIndent(),
            )
            val parsed = Json.parseToJsonElement(
                Json.parseToJsonElement(result).jsonPrimitive.content,
            ).jsonObject

            assertEquals("First paragraph.", parsed["first"]!!.jsonPrimitive.content)
            assertFalse(parsed["firstHasTerminalNewline"]!!.jsonPrimitive.content.toBoolean())
            assertFalse(parsed["scrollChanged"]!!.jsonPrimitive.content.toBoolean())
            val across = parsed["across"]!!.jsonPrimitive.content
            assertTrue(across.startsWith("First paragraph."))
            assertTrue(across.endsWith("Second paragraph."))

            evaluate(
                scenario,
                """
                    (() => {
                      const range = document.createRange();
                      range.selectNodeContents(document.body);
                      const selection = getSelection();
                      selection.removeAllRanges();
                      selection.addRange(range);
                      return true;
                    })()
                """.trimIndent(),
            )
            Thread.sleep(250)
            val selectAll = Json.parseToJsonElement(
                evaluate(scenario, "getSelection().toString()"),
            ).jsonPrimitive.content
            assertTrue(selectAll.contains("First paragraph."))
            assertTrue(selectAll.contains("Second paragraph."))
            assertFalse(selectAll.contains("No previous chapter"))
            assertFalse(selectAll.contains("No next chapter"))

            scenario.onActivity { controller.destroy() }
        }
    }

    private fun evaluate(
        scenario: ActivityScenario<ContinuousProseWebViewTestActivity>,
        script: String,
    ): String {
        val completed = CountDownLatch(1)
        var result: String? = null
        scenario.onActivity {
            it.container.getChildAt(0).let { view ->
                (view as android.webkit.WebView).evaluateJavascript(script) { value ->
                    result = value
                    completed.countDown()
                }
            }
        }
        assertTrue("JavaScript evaluation timed out", completed.await(10, TimeUnit.SECONDS))
        return requireNotNull(result)
    }
}

class ContinuousProseWebViewTestActivity : Activity() {
    lateinit var container: FrameLayout
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = FrameLayout(this)
        setContentView(container)
    }
}
