package mihon.entry.interactions.book.document.reader.paging

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.reader.BookDocumentChapterSelectionContainer
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTextScale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentPageTextMeasurementTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun prose_measurements_match_rendered_typography_and_page_edge_spacing() {
        val section = pagingHtmlSection(
            "<h2>A heading with enough words to wrap onto several lines</h2>" +
                "<blockquote><p>A quoted passage with <em>emphasis</em> and more words to wrap.</p></blockquote>" +
                "<p style='padding: 0.4em; margin-top: 1.2em; margin-bottom: 0.7em; text-indent: 1.5em'>" +
                "Indented <strong>bold</strong> text and <small>small words</small>, 日本語 and שלום.</p>" +
                "<p>Another paragraph with several lines of text. More words to test the final line.</p>",
        )
        val fragments = section.viewerBlocks.flatMap { block ->
            listOf(false, true).flatMap { first ->
                listOf(false, true).map { last ->
                    BookDocumentPageFragment(block, firstOnPage = first, lastOnPage = last)
                }
            }
        }
        val index = mutableIntStateOf(0)
        val scale = mutableFloatStateOf(1f)
        val width = mutableStateOf(273.dp)
        var actual = 0
        var expected = 0
        compose.setContent {
            PagingTheme {
                CompositionLocalProvider(LocalBookDocumentTextScale provides scale.floatValue) {
                    val measurer = rememberBookDocumentPageTextMeasurer(section.viewerBlocks)
                    val pixels = with(compose.density) { width.value.roundToPx() }
                    val fragment = fragments[index.intValue]
                    SideEffect {
                        measurer.retain(section.viewerBlocks, pixels)
                        expected = measurer.measure(fragment).first
                    }
                    BookDocumentChapterSelectionContainer(1L) {
                        Box(Modifier.width(width.value).onSizeChanged { actual = it.height }) {
                            BookDocumentPageFragmentContent(fragment, emptyMap(), { _, _ -> }, {}, {}, {})
                        }
                    }
                }
            }
        }
        for ((textScale, readingWidth) in listOf(1f to 273.dp, 1.45f to 273.dp, 1.45f to 321.dp)) {
            fragments.indices.forEach { fragmentIndex ->
                compose.runOnIdle {
                    index.intValue = fragmentIndex
                    scale.floatValue = textScale
                    width.value = readingWidth
                }
                compose.waitForIdle()
                assertEquals("Fragment $fragmentIndex at $textScale / $readingWidth", actual, expected)
            }
        }
    }
}
