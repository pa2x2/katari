package mihon.entry.interactions.book.document.reader.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.reader.BookDocumentModeViewport
import mihon.entry.interactions.book.document.reader.BookDocumentPublicationSections
import mihon.entry.interactions.book.document.reader.BookDocumentReaderState
import mihon.entry.interactions.book.document.reader.paging.PagingTheme
import mihon.entry.interactions.book.document.reader.paging.pagingHtmlSection
import mihon.entry.interactions.book.navigation.BookChapterReadingOrder
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentSeekAvailabilityTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun image_only_sections_still_expose_navigation_without_text_geometry() {
        val section = pagingHtmlSection("<img src='cover.png' />")
        val seek = BookDocumentSeekState()
        val state = BookDocumentReaderState(
            entryTitle = "Illustrated book",
            readingOrder = BookChapterReadingOrder(listOf(section.owner)),
            currentChapterId = section.owner.id,
            window = EntryChildWindow(section.owner),
            loadedSections = mapOf(section.owner.id to BookDocumentPublicationSections(listOf(section), section.key)),
            chromeVisible = true,
        )
        compose.setContent {
            PagingTheme {
                SideEffect { seek.setVisible(true) }
                Column {
                    BookDocumentModeViewport(
                        currentChapter = section.owner,
                        currentChapterId = section.owner.id,
                        window = state.window,
                        loadedSections = state.loadedSections,
                        loadStates = emptyMap(),
                        navigationRequest = null,
                        textSizePercent = 100,
                        onLocation = {},
                        onTransitionReached = {},
                        onTerminalObservation = { _, _, _, _ -> },
                        onAnchorMissing = {},
                        onInternalLinkClick = { _, _ -> },
                        onExternalLinkClick = {},
                        onScrollStarted = {},
                        onUserScrollStarted = {},
                        onReaderTap = {},
                        mode = BookDocumentReadingMode.SCROLL,
                        tapZones = 0,
                        inversion = 0,
                        animation = false,
                        volume = false,
                        invertVolume = false,
                        chromeVisible = true,
                        modifier = Modifier.size(320.dp, 320.dp),
                        onViewportLocation = seek::observe,
                    )
                    BookDocumentSeekControls(seek, state, BookDocumentReadingMode.SCROLL, BookDocumentJumpHistory(), {
                    }, {})
                }
            }
        }
        compose.waitUntil(5_000) { seek.snapshot != null }
        compose.onNodeWithText("Chapter position").assertIsDisplayed()
    }
}
