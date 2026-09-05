package mihon.entry.interactions.book.document.reader.paging

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.reader.BookDocumentChapterSelectionContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentPageGeometryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun prose_pages_have_consistent_top_edges_and_no_sparse_middle_pages() {
        val section = pagingHtmlSection(
            (1..45).joinToString("") {
                "<p>Paragraph $it has several words that wrap across lines, followed by more prose for the next page.</p>"
            },
        )
        var pages = emptyList<BookDocumentPage>()
        val index = mutableStateOf(0)
        compose.setContent {
            PagingTheme {
                BookDocumentPaginationLayout(section.viewerBlocks, Modifier.size(280.dp, 300.dp)) { measured ->
                    SideEffect { pages = measured }
                    BookDocumentChapterSelectionContainer(1L) {
                        BookDocumentPageContent(measured[index.value], emptyMap(), { _, _ -> }, {}, {}, {})
                    }
                }
            }
        }
        compose.waitForIdle()
        var firstTop: Float? = null
        pages.indices.forEach { pageIndex ->
            compose.runOnIdle { index.value = pageIndex }
            compose.waitForIdle()
            val nodes = compose.onAllNodes(
                androidx.compose.ui.test.hasText("", substring = true),
                useUnmergedTree = true,
            )
                .fetchSemanticsNodes().filter { node ->
                    node.layoutInfo.isPlaced && node.boundsInRoot.height > 0 &&
                        generateSequence(node.parent) { it.parent }.none { it.config.isClearingSemantics }
                }
            val top = nodes.minOf { it.boundsInRoot.top }
            if (firstTop == null) firstTop = top
            assertEquals("Page ${pageIndex + 1} top edge", requireNotNull(firstTop), top, 1f)
            val height = nodes.maxOf { it.boundsInRoot.bottom } - top
            if (pageIndex < pages.lastIndex) {
                assertTrue(
                    "Middle page ${pageIndex + 1} must use the available prose area",
                    height > 200 * compose.activity.resources.displayMetrics.density,
                )
            }
            assertTrue(
                "Text must fit inside the page",
                height <= 300 * compose.activity.resources.displayMetrics.density,
            )
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
                result.first().fragments.first().renderedTextBlock().let { block ->
                    section.document.document.content.text.substring(block.logicalStart, block.logicalEndExclusive)
                },
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
