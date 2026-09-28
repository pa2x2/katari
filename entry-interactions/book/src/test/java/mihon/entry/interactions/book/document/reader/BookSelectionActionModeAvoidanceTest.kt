package mihon.entry.interactions.book.document.reader

import androidx.compose.ui.geometry.Rect
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class BookSelectionActionModeAvoidanceTest {
    private val selectionBounds = Rect(left = 100f, top = 400f, right = 200f, bottom = 450f)

    @Test
    fun `popup appearing, dismissing or growing away from the selection repositions the native menu`() {
        val below = Rect(left = 80f, top = 474f, right = 220f, bottom = 550f)
        val above = Rect(left = 80f, top = 250f, right = 220f, bottom = 376f)

        requiresActionModeReposition(selectionBounds, null, below) shouldBe true
        requiresActionModeReposition(selectionBounds, below, null) shouldBe true
        requiresActionModeReposition(selectionBounds, below, below.copy(bottom = 700f)) shouldBe true
        requiresActionModeReposition(selectionBounds, above, above.copy(top = 150f)) shouldBe true
    }

    @Test
    fun `popup changes that leave the selected content extent in place keep the native menu stable`() {
        val below = Rect(left = 80f, top = 474f, right = 220f, bottom = 700f)

        requiresActionModeReposition(selectionBounds, below, below) shouldBe false
        requiresActionModeReposition(selectionBounds, below, below.copy(bottom = 550f)) shouldBe false
        requiresActionModeReposition(selectionBounds, below, below.copy(left = 40f, right = 260f)) shouldBe false
        // A popup above the selection growing into it keeps the union of the content unchanged.
        requiresActionModeReposition(
            Rect(left = 369f, top = 965f, right = 382f, bottom = 1126f),
            Rect(left = 36f, top = 730f, right = 837f, bottom = 911f),
            Rect(left = 36f, top = 730f, right = 837f, bottom = 1007f),
        ) shouldBe false
    }
}
