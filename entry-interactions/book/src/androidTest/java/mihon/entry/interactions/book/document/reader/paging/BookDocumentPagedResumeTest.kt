package mihon.entry.interactions.book.document.reader.paging

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.key
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
import mihon.entry.interactions.book.document.reader.BookDocumentModeViewport
import mihon.entry.interactions.book.document.reader.BookDocumentPublicationSections
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import mihon.entry.interactions.book.reader.BookReaderProgress
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentPagedResumeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun resuming_inside_a_page_does_not_insert_a_short_page_before_the_saved_passage() {
        val original = pagingSection((1..150).joinToString(" ") { "Word$it follows the previous words." })
        val section = mutableStateOf(original)
        var location = BookDocumentViewerLocation(original, original.initialPosition, 0f)
        var progress: BookReaderProgress.Page? = null
        compose.setContent {
            PagingTheme {
                key(section.value.initialPosition) {
                    BookDocumentModeViewport(
                        currentChapter = original.owner,
                        currentChapterId = original.owner.id,
                        window = EntryChildWindow(original.owner),
                        loadedSections = mapOf(
                            1L to BookDocumentPublicationSections(listOf(section.value), original.key),
                        ),
                        loadStates = emptyMap(),
                        navigationRequest = null,
                        textSizePercent = 100,
                        onLocation = { location = it },
                        onTransitionReached = {},
                        onTerminalObservation = { _, _, _, _ -> },
                        onAnchorMissing = {},
                        onInternalLinkClick = { _, _ -> },
                        onExternalLinkClick = {},
                        onScrollStarted = {},
                        onUserScrollStarted = {},
                        onReaderTap = {},
                        mode = BookDocumentReadingMode.PAGED_LTR,
                        tapZones = 0,
                        inversion = 0,
                        animation = false,
                        volume = false,
                        invertVolume = false,
                        chromeVisible = false,
                        modifier = Modifier.size(280.dp, 300.dp).testTag("pager"),
                        onPageProgress = { progress = it },
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.waitUntil(5_000) { progress != null }
        val count = requireNotNull(progress).totalPages
        compose.onNodeWithTag("pager").performTouchInput { click(Offset(width * .9f, height * .5f)) }
        compose.waitForIdle()
        assertEquals(2, progress?.currentPage)
        assertTrue(location.position.offsetWithinBlock > 0)
        val savedPosition = location.position.copy(offsetWithinBlock = location.position.offsetWithinBlock + 6)
        compose.runOnIdle { section.value = original.copy(initialPosition = savedPosition) }
        compose.waitForIdle()
        compose.waitUntil(5_000) { location.position == savedPosition }
        assertEquals(savedPosition, location.position)
        assertEquals(BookReaderProgress.Page(2, count), progress)
    }
}
