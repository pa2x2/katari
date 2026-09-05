package mihon.entry.interactions.book.document.reader.paging

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentPagePreparationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun reloading_a_resource_revision_replaces_geometry_even_when_block_ids_and_ranges_match() {
        val original = pagingSection("i ".repeat(400))
        val revised = pagingSection("W ".repeat(400)).let {
            it.copy(document = PreparedBookDocument(it.document.document.copy(revision = "r2")))
        }
        val section = mutableStateOf(original)
        var pages = emptyList<BookDocumentPage>()
        compose.setContent {
            PagingTheme {
                BookDocumentPaginationLayout(section.value.viewerBlocks, Modifier.size(280.dp, 240.dp)) { prepared ->
                    SideEffect { pages = prepared }
                    Box {}
                }
            }
        }
        compose.waitUntil(5_000) { pages.isNotEmpty() }
        val originalCount = pages.size
        compose.runOnIdle { section.value = revised }
        compose.waitUntil(5_000) { pages.firstOrNull()?.fragments?.firstOrNull()?.section === revised }
        compose.runOnIdle {
            assertTrue("The wider replacement text must be repaginated", pages.size > originalCount)
            val source = revised.document.document.content.text
            assertEquals(source, pages.flatMap { it.fragments }.joinToString("") { source.substring(it.start, it.end) })
        }
    }
}
