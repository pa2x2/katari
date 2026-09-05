package mihon.entry.interactions.book.document.reader.paging

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.book.api.document.BookDocumentPosition
import mihon.entry.interactions.book.document.reader.BookDocumentChapterSelectionContainer
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentPaginationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun long_paragraph_is_partitioned_without_missing_or_repeated_unicode_text() {
        val text = (1..90).joinToString("\n") { "Line $it: café 日本語 😀 words to wrap." }
        val section = pagingSection(text)
        val pageBreak = mutableStateOf<BookDocumentViewerLocation<tachiyomi.domain.entry.model.EntryChapter>?>(null)
        var result = emptyList<BookDocumentPage>()
        compose.setContent {
            PagingTheme {
                BookDocumentPaginationLayout(
                    section.viewerBlocks,
                    Modifier.size(280.dp, 220.dp),
                    pageBreak = pageBreak.value,
                ) { pages ->
                    SideEffect { result = pages }
                    Box {}
                }
            }
        }
        val handoff = BookDocumentPosition(section.initialPosition.blockId, text.indexOf("Line 44:"))
        val anchored = BookDocumentViewerLocation(section, handoff, section.document.document.progressionAt(handoff))
        listOf(null, anchored).forEach { boundary ->
            compose.runOnIdle { pageBreak.value = boundary }
            compose.waitForIdle()
            compose.runOnIdle {
                assertTrue(result.size > 3)
                assertTrue(result.none { it.scrollable })
                val fragments = result.flatMap { it.fragments }
                assertEquals(text, fragments.joinToString("") { text.substring(it.start, it.end) })
                fragments.zipWithNext().forEach { (first, second) -> assertEquals(first.end, second.start) }
                fragments.forEach { assertFalse(Character.isLowSurrogate(text[it.start])) }
                if (boundary != null) {
                    assertTrue(
                        "The handoff line must start a page",
                        result.any {
                            it.fragments.first().position ==
                                handoff
                        },
                    )
                }
            }
        }
    }

    @Test fun displayed_fragments_fit_the_same_page_height_as_measurement() {
        val section = pagingSection((1..35).joinToString(" ") { "Paragraph word $it." })
        var result = emptyList<BookDocumentPage>()
        compose.setContent {
            PagingTheme {
                BookDocumentPaginationLayout(section.viewerBlocks, Modifier.size(280.dp, 180.dp)) { pages ->
                    SideEffect { result = pages }
                    BookDocumentChapterSelectionContainer(1L) {
                        BookDocumentPageContent(pages.first(), emptyMap(), { _, _ -> }, {}, {}, {})
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(result.size > 1)
            assertFalse(result.first().scrollable)
        }
        // Actual visible text geometry is checked through Compose's public text-layout semantics.
        val nodes = compose.onAllNodes(
            androidx.compose.ui.test.hasText(
                section.document.document.content.text.substring(0, result.first().fragments.first().end),
            ),
        )
        nodes[0].performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { action ->
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            assertTrue(action(layouts))
            assertFalse(layouts.single().hasVisualOverflow)
            val density = compose.activity.resources.displayMetrics.density
            assertTrue(layouts.single().size.height <= 180 * density)
        }
    }
}
