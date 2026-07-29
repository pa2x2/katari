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
import java.util.Collections
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
            bodyHtml = "<p>First paragraph.</p><p>Second paragraph.</p><p>First paragraph.</p>",
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
            labels = ContinuousProseTransitionLabels(
                noPrevious = "No previous chapter",
                noNext = "No next chapter",
                loading = "Loading",
                retry = "Retry",
                loadFailed = "Failed",
            ),
        )
        val prepared = CountDownLatch(1)
        val selections = Collections.synchronizedList(mutableListOf<ContinuousProseEvent.Selection>())
        lateinit var controller: ContinuousProseWebViewController

        ActivityScenario.launch(ContinuousProseWebViewTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                controller = ContinuousProseWebViewController(activity)
                controller.callbacks = {
                    ContinuousProseWebViewCallbacks(
                        onEvent = { event, _ ->
                            if (event is ContinuousProseEvent.Prepared) prepared.countDown()
                            if (event is ContinuousProseEvent.Selection) selections += event
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

            fun selectParagraph(index: Int): String {
                evaluate(
                    scenario,
                    """
                        (() => {
                          const paragraph = document.querySelectorAll('.section.current p.block')[$index];
                          const range = document.createRange();
                          range.selectNodeContents(paragraph);
                          const selection = getSelection();
                          selection.removeAllRanges();
                          selection.addRange(range);
                          return true;
                        })()
                    """.trimIndent(),
                )
                Thread.sleep(250)
                return selections.last().identity
            }
            assertFalse(
                "Equal text ranges in different paragraphs must have distinct identities",
                selectParagraph(0) == selectParagraph(2),
            )

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

    @Test
    fun transitionUpdatesPreserveTheBoundaryAndTerminalBoundaryCompletesTheChapter() {
        val current = chapterSection(1, "<p>Current chapter</p>")
        val next = chapterSection(
            2,
            (1..30).joinToString("") { index -> "<p>Next chapter $index</p>" },
        )
        val labels = testLabels()
        val initialProjection = buildContinuousProseProjection(
            generation = 10,
            window = EntryChildWindow(current.owner, null, next.owner),
            loaded = mapOf(current.owner.id to current),
            labels = labels,
        )
        val firstPrepared = CountDownLatch(1)
        val secondPrepared = CountDownLatch(1)
        val loadedPrepared = CountDownLatch(1)
        val crossedPrepared = CountDownLatch(1)
        val terminalPosition = CountDownLatch(1)
        lateinit var controller: ContinuousProseWebViewController

        ActivityScenario.launch(ContinuousProseWebViewTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                controller = ContinuousProseWebViewController(activity)
                controller.callbacks = {
                    ContinuousProseWebViewCallbacks(
                        onEvent = { event, _ ->
                            when (event) {
                                is ContinuousProseEvent.Prepared -> when (event.generation) {
                                    10L -> firstPrepared.countDown()
                                    11L -> secondPrepared.countDown()
                                    12L -> loadedPrepared.countDown()
                                    13L -> crossedPrepared.countDown()
                                }
                                is ContinuousProseEvent.Position -> {
                                    if (event.generation == 13L && event.progression == 1f) {
                                        terminalPosition.countDown()
                                    }
                                }
                                else -> Unit
                            }
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
                    projection = initialProjection,
                    sections = mapOf(current.key to current),
                    command = renderCommand(initialProjection, current),
                )
            }
            assertTrue(firstPrepared.await(10, TimeUnit.SECONDS))
            centerNextTransition(scenario)
            val before = transitionTop(scenario)

            val loadingProjection = buildContinuousProseProjection(
                generation = 11,
                window = EntryChildWindow(current.owner, null, next.owner),
                loaded = mapOf(current.owner.id to current),
                labels = labels,
                transitionStates = mapOf(next.owner.id to ContinuousProseTransitionState.Loading),
            )
            scenario.onActivity {
                controller.update(
                    projection = loadingProjection,
                    sections = mapOf(current.key to current),
                    command = renderCommand(loadingProjection, current),
                )
            }
            assertTrue(secondPrepared.await(10, TimeUnit.SECONDS))
            val after = transitionTop(scenario)
            assertTrue("Transition moved from $before to $after", kotlin.math.abs(after - before) < 2f)

            val loadedProjection = buildContinuousProseProjection(
                generation = 12,
                window = EntryChildWindow(current.owner, null, next.owner),
                loaded = mapOf(current.owner.id to current, next.owner.id to next),
                labels = labels,
            )
            scenario.onActivity {
                controller.update(
                    projection = loadedProjection,
                    sections = mapOf(current.key to current, next.key to next),
                    command = renderCommand(loadedProjection, current),
                )
            }
            assertTrue(loadedPrepared.await(10, TimeUnit.SECONDS))
            val afterInsertion = transitionTop(scenario)
            assertTrue(
                "Transition moved after destination insertion from $after to $afterInsertion",
                kotlin.math.abs(afterInsertion - after) < 2f,
            )

            centerSectionBlock(scenario, next.key)
            val blockBeforeCrossing = sectionBlockTop(scenario, next.key)
            val crossedProjection = buildContinuousProseProjection(
                generation = 13,
                window = EntryChildWindow(next.owner, current.owner, null),
                loaded = mapOf(current.owner.id to current, next.owner.id to next),
                labels = labels,
            )
            scenario.onActivity {
                controller.update(
                    projection = crossedProjection,
                    sections = mapOf(current.key to current, next.key to next),
                    command = renderCommand(crossedProjection, next),
                )
            }
            assertTrue(crossedPrepared.await(10, TimeUnit.SECONDS))
            val blockAfterCrossing = sectionBlockTop(scenario, next.key)
            assertTrue(
                "Entered chapter block moved from $blockBeforeCrossing to $blockAfterCrossing",
                kotlin.math.abs(blockAfterCrossing - blockBeforeCrossing) < 2f,
            )

            centerNextTransition(scenario)
            assertTrue(
                "Terminal boundary did not report chapter completion",
                terminalPosition.await(10, TimeUnit.SECONDS),
            )
            scenario.onActivity { controller.destroy() }
        }
    }

    private fun chapterSection(
        id: Long,
        html: String,
    ): BookDocumentSection<EntryChapter> {
        val chapter = EntryChapter.create().copy(id = id, entryId = 2L, name = "Chapter $id")
        val document = prepareHtmlBookDocument("chapter-$id", "r1", html)
        return BookDocumentSection(
            key = id.toString(),
            owner = chapter,
            document = document,
            initialPosition = document.document.positionAtProgression(0f),
            resourceLoader = null,
        )
    }

    private fun testLabels() = ContinuousProseTransitionLabels(
        noPrevious = "No previous chapter",
        noNext = "No next chapter",
        loading = "Loading",
        retry = "Retry",
        loadFailed = "Failed",
    )

    private fun renderCommand(
        projection: ContinuousProseProjection,
        section: BookDocumentSection<EntryChapter>,
    ) = continuousProseRenderCommand(
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
    )

    private fun centerNextTransition(
        scenario: ActivityScenario<ContinuousProseWebViewTestActivity>,
    ) {
        evaluate(
            scenario,
            """
                (() => {
                  const transition = document.querySelector('.transition[data-direction="next"]');
                  scrollTo(0, scrollY + transition.getBoundingClientRect().top -
                    innerHeight / 2 + transition.getBoundingClientRect().height / 2);
                  return true;
                })()
            """.trimIndent(),
        )
        Thread.sleep(250)
    }

    private fun transitionTop(
        scenario: ActivityScenario<ContinuousProseWebViewTestActivity>,
    ): Float = Json.parseToJsonElement(
        evaluate(
            scenario,
            "document.querySelector('.transition[data-direction=\"next\"]').getBoundingClientRect().top",
        ),
    ).jsonPrimitive.content.toFloat()

    private fun centerSectionBlock(
        scenario: ActivityScenario<ContinuousProseWebViewTestActivity>,
        sectionKey: String,
    ) {
        evaluate(
            scenario,
            """
                (() => {
                  const block = document.querySelector(
                    '.section[data-section-key="$sectionKey"] .block:nth-of-type(11)'
                  ) || document.querySelector('.section[data-section-key="$sectionKey"] .block');
                  scrollTo(0, scrollY + block.getBoundingClientRect().top -
                    innerHeight / 2 + block.getBoundingClientRect().height / 2);
                  return true;
                })()
            """.trimIndent(),
        )
        Thread.sleep(250)
    }

    private fun sectionBlockTop(
        scenario: ActivityScenario<ContinuousProseWebViewTestActivity>,
        sectionKey: String,
    ): Float = Json.parseToJsonElement(
        evaluate(
            scenario,
            """
                document.querySelector(
                  '.section[data-section-key="$sectionKey"] .block:nth-of-type(11)'
                )?.getBoundingClientRect().top ??
                  document.querySelector(
                    '.section[data-section-key="$sectionKey"] .block'
                  ).getBoundingClientRect().top
            """.trimIndent(),
        ),
    ).jsonPrimitive.content.toFloat()

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
