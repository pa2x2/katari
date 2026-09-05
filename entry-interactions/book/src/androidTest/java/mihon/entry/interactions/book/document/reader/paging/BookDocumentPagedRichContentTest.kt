package mihon.entry.interactions.book.document.reader.paging

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.reader.BookDocumentChapterSelectionContainer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentPagedRichContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun expanding_a_note_keeps_its_end_accessible_without_covering_the_following_page() {
        val section = pagingHtmlSection(
            "<details><summary>Open note</summary>" +
                (1..20).joinToString("") { "<p>Note paragraph $it.</p>" } +
                "<p>Last note line.</p></details><p>After note.</p>",
        )
        val index = mutableIntStateOf(0)
        compose.setContent {
            PagingTheme {
                BookDocumentPaginationLayout(section.viewerBlocks, Modifier.size(280.dp, 180.dp)) { pages ->
                    BookDocumentChapterSelectionContainer(1L) {
                        BookDocumentPageContent(pages[index.intValue], emptyMap(), { _, _ -> }, {}, {}, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Open note").performClick()
        compose.onNodeWithText("Last note line.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { index.intValue = 1 }
        compose.onNodeWithText("After note.").assertIsDisplayed()
    }
}
