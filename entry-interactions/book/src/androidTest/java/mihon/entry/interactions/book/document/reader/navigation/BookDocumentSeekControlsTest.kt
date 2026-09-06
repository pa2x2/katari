package mihon.entry.interactions.book.document.reader.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.book.api.document.resolvePosition
import mihon.entry.interactions.book.document.reader.BookDocumentModeViewport
import mihon.entry.interactions.book.document.reader.BookDocumentNavigationRequest
import mihon.entry.interactions.book.document.reader.BookDocumentPublicationSections
import mihon.entry.interactions.book.document.reader.BookDocumentReaderState
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTextScale
import mihon.entry.interactions.book.document.reader.acceptsLocation
import mihon.entry.interactions.book.document.reader.paging.PagingTheme
import mihon.entry.interactions.book.document.reader.paging.pagingSection
import mihon.entry.interactions.book.navigation.BookChapterReadingOrder
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentSeekControlsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun scrolling_seeks_commit_on_release_and_return_restores_the_previous_passage() {
        verifySeek(BookDocumentReadingMode.SCROLL)
    }

    @Test fun paged_seek_commits_on_release_and_return_survives_font_reflow() {
        verifySeek(BookDocumentReadingMode.PAGED_LTR)
    }

    @Test fun seeking_to_the_end_stays_in_the_section_when_the_next_chapter_is_prefetched() {
        verifySeek(BookDocumentReadingMode.SCROLL, seekEnd = true)
    }

    private fun verifySeek(mode: BookDocumentReadingMode, seekEnd: Boolean = false) {
        val section = pagingSection((1..200).joinToString(" ") { "Word$it follows the previous words." })
        val next = pagingSection("Next chapter has different content.", 2)
        val document = section.document.document
        val seek = BookDocumentSeekState()
        val history = BookDocumentJumpHistory()
        val request = mutableStateOf<BookDocumentNavigationRequest?>(null)
        val scale = mutableFloatStateOf(1f)
        var committed = section.initialPosition
        var deferNavigation = true
        var deferredRequest: BookDocumentNavigationRequest? = null
        var jumps = 0L
        var activeChapter = section.owner.id
        val state = BookDocumentReaderState(
            entryTitle = "Book",
            readingOrder = BookChapterReadingOrder(listOf(section.owner, next.owner)),
            currentChapterId = section.owner.id,
            window = EntryChildWindow(section.owner, next = next.owner),
            loadedSections = mapOf(
                section.owner.id to BookDocumentPublicationSections(listOf(section), section.key),
                next.owner.id to BookDocumentPublicationSections(listOf(next), next.key),
            ),
            chromeVisible = true,
        )
        fun navigate(target: BookDocumentNavigationTarget) {
            if (!target.returnToOrigin) history.rememberOrigin()
            jumps++
            val nextRequest = BookDocumentNavigationRequest(
                jumps,
                section.owner.id,
                requireNotNull(document.resolvePosition(requireNotNull(target.locator))),
                section.key,
                alignToPassage = true,
                returnToOrigin = target.returnToOrigin,
            )
            if (deferNavigation) deferredRequest = nextRequest else request.value = nextRequest
        }
        compose.setContent {
            PagingTheme {
                CompositionLocalProvider(LocalBookDocumentTextScale provides scale.floatValue) {
                    SideEffect { seek.setVisible(true) }
                    Column {
                        BookDocumentModeViewport(
                            currentChapter = section.owner,
                            currentChapterId = section.owner.id,
                            window = state.window,
                            loadedSections = state.loadedSections,
                            loadStates = emptyMap(),
                            navigationRequest = request.value,
                            textSizePercent = (scale.floatValue * 100).toInt(),
                            onLocation = {
                                if (request.value.acceptsLocation(
                                        it.section.owner.id,
                                        it.position,
                                        it.section.key,
                                        it.restoredNavigationId,
                                    )
                                ) {
                                    activeChapter = it.section.owner.id
                                    committed = it.position
                                    if (request.value?.returnToOrigin == true) history.dismiss()
                                    request.value = null
                                }
                            },
                            onTransitionReached = {},
                            onTerminalObservation = { _, _, _, _ -> },
                            onAnchorMissing = {},
                            onInternalLinkClick = { _, _ -> },
                            onExternalLinkClick = {},
                            onScrollStarted = {},
                            onUserScrollStarted = { request.value = null },
                            onReaderTap = {},
                            mode = mode,
                            tapZones = 0,
                            inversion = 0,
                            animation = false,
                            volume = false,
                            invertVolume = false,
                            chromeVisible = true,
                            modifier = Modifier.size(320.dp, 320.dp).testTag("viewport"),
                            onSeekPages = seek::updatePages,
                            onViewportLocation = {
                                seek.observe(it)
                                history.observe(it)
                            },
                        )
                        BookDocumentSeekControls(
                            seek,
                            state,
                            mode,
                            history,
                            ::navigate,
                            onReturn = { history.returnTarget?.copy(returnToOrigin = true)?.let(::navigate) },
                        )
                    }
                }
            }
        }
        compose.waitUntil(5_000) {
            seek.snapshot != null &&
                (mode == BookDocumentReadingMode.SCROLL || seek.snapshot?.paged == true)
        }
        compose.waitForIdle()
        val original = committed
        val slider = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        if (mode == BookDocumentReadingMode.SCROLL && !seekEnd) {
            slider.performTouchInput {
                down(Offset(width * .05f, height * .5f))
                moveTo(Offset(width * .6f, height * .5f), 400)
            }
            compose.waitForIdle()
            slider.performTouchInput { cancel() }
            compose.waitForIdle()
            assertEquals("Cancelling a preview must not commit a jump", 0L, jumps)
            assertEquals(original, committed)
        }
        slider.performTouchInput {
            down(Offset(width * .05f, height * .5f))
            moveTo(Offset(width * (if (seekEnd) 1f else .7f), height * .5f), 500)
        }
        compose.waitForIdle()
        assertEquals("Holding a preview must not navigate or persist progress", 0L, jumps)
        assertEquals(original, committed)
        val previewValue = slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
        slider.performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(
            "Release keeps the selected slider value while the viewer still reports the old passage",
            previewValue,
            slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current,
            .01f,
        )
        compose.runOnIdle {
            deferNavigation = false
            request.value = deferredRequest
        }
        compose.waitUntil(5_000) { jumps == 1L && request.value == null }
        compose.waitForIdle()
        assertTrue("The release must reach a later passage", (document.logicalOffset(committed) ?: 0) > 100)
        assertEquals("A local seek must not activate the prefetched chapter", section.owner.id, activeChapter)
        if (seekEnd) assertEquals(100f, requireNotNull(seek.snapshot).value, .01f)
        if (!seekEnd) {
            val previousPassage = requireNotNull(seek.snapshot).position
            slider.performTouchInput {
                down(Offset(width * .7f, height * .5f))
                moveTo(Offset(width * .95f, height * .5f), 500)
                up()
            }
            compose.waitUntil(5_000) { jumps == 2L && request.value == null }
            compose.waitForIdle()
            assertTrue("The second jump must reach another passage", committed != previousPassage)
            assertEquals(
                "A repeated jump remembers the passage before that jump",
                previousPassage,
                document.resolvePosition(requireNotNull(history.returnTarget?.locator)),
            )
        }
        val origin = requireNotNull(history.returnTarget)
        if (mode != BookDocumentReadingMode.SCROLL) {
            compose.runOnIdle { scale.floatValue = 1.5f }
            compose.waitForIdle()
        }
        compose.onNodeWithText("Return to previous position").performClick()
        compose.waitUntil(5_000) { jumps == (if (seekEnd) 2L else 3L) && request.value == null }
        compose.waitForIdle()
        compose.onNodeWithText("Return to previous position").assertDoesNotExist()
        assertEquals(
            "Return restores the first visible passage; reading progress may observe the viewport center",
            document.resolvePosition(requireNotNull(origin.locator)),
            requireNotNull(seek.snapshot).position,
        )
    }
}
