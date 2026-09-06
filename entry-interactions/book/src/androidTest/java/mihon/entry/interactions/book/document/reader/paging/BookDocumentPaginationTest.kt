package mihon.entry.interactions.book.document.reader.paging

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        var result = emptyList<BookDocumentPage>()
        compose.setContent {
            PagingTheme {
                BookDocumentPaginationLayout(
                    section.viewerBlocks,
                    Modifier.size(280.dp, 220.dp),
                ) { pages ->
                    SideEffect { result = pages }
                    Box {}
                }
            }
        }
        compose.waitForIdle()
        compose.waitUntil(5_000) { result.isNotEmpty() }
        compose.runOnIdle {
            assertTrue(result.size > 3)
            assertTrue(result.none { it.scrollable })
            val fragments = result.flatMap { it.fragments }
            assertEquals(text, fragments.joinToString("") { text.substring(it.start, it.end) })
            fragments.zipWithNext().forEach { (first, second) -> assertEquals(first.end, second.start) }
            fragments.forEach { assertFalse(Character.isLowSurrogate(text[it.start])) }
        }
    }
}
