package mihon.entry.interactions.book.document.reader.paging

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.reader.BookDocumentChapterLoadState
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import mihon.entry.interactions.book.document.reader.buildBookDocumentViewerItems
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.domain.entry.model.EntryChapter

@RunWith(AndroidJUnit4::class)
class BookDocumentPagedChapterLoadingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun forward_taps_resume_when_the_chapter_loads_after_a_transition_tap() {
        val first = pagingSection("First chapter text.")
        val next = pagingSection("Next chapter text.", 2)
        val loaded = mutableStateOf(false)
        val initial = BookDocumentViewerLocation(first, first.initialPosition, 0f)
        var location = initial
        var requestedChapter: Long? = null
        var pages = emptyList<BookDocumentPage>()
        compose.setContent {
            PagingTheme {
                val items = buildBookDocumentViewerItems(
                    EntryChildWindow(first.owner, next = next.owner),
                    if (loaded.value) mapOf(1L to first, 2L to next) else mapOf(1L to first),
                    EntryChapter::id,
                )
                BookDocumentPaginationLayout(items, Modifier.size(280.dp, 400.dp).testTag("pager")) { prepared ->
                    SideEffect { pages = prepared }
                    BookDocumentPagedViewer(
                        prepared, BookDocumentReadingMode.PAGED_LTR, initial, null,
                        if (loaded.value) emptyMap() else mapOf(2L to BookDocumentChapterLoadState.Loading),
                        0, 0, false, false, false, false,
                        { location = it }, { requestedChapter = it.id }, { _, _, _, _ -> }, { _, _ -> }, {}, {}, {}, {},
                    )
                }
            }
        }
        compose.waitUntil(5_000) { pages.isNotEmpty() }
        fun tapForward() {
            compose.onNodeWithTag("pager").performTouchInput { click(Offset(width * .9f, height * .8f)) }
            compose.waitForIdle()
        }
        tapForward()
        assertEquals(2L, requestedChapter)
        // Start the transition's tap handler while its destination is still unavailable.
        tapForward()
        assertEquals(1L, location.section.owner.id)
        compose.runOnIdle { loaded.value = true }
        compose.waitUntil(5_000) { pages.any { it.fragments.first().section?.owner?.id == 2L } }
        compose.waitForIdle()
        tapForward()
        assertEquals(
            "The loaded chapter must be reachable without changing input methods",
            2L,
            location.section.owner.id,
        )
    }
}
