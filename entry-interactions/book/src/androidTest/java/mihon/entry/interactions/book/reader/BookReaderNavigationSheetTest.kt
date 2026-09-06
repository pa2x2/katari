package mihon.entry.interactions.book.reader

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.child.EntryChildProgressLabel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.i18n.MR

@RunWith(AndroidJUnit4::class)
class BookReaderNavigationSheetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun current_section_stays_visible_when_progress_labels_arrive() {
        val rows = mutableStateOf((1..40).map { BookReaderNavigationRow(it, "Chapter $it") })
        composeRule.setContent {
            MaterialTheme {
                BookReaderNavigationSheet(true, rows.value, 39, {}, {})
            }
        }
        composeRule.onNodeWithText("Chapter 40").assertIsDisplayed()
        composeRule.runOnIdle {
            rows.value = rows.value.map { it.copy(progressLabel = EntryChildProgressLabel(MR.strings.label_started)) }
        }
        composeRule.onNodeWithText("Chapter 40").assertIsDisplayed()
    }
}
