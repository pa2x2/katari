package mihon.entry.interactions.book.document.reader

import androidx.compose.ui.geometry.Rect
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class BookSelectionActionModeAvoidanceTest {
    private val selectionBounds = Rect(left = 100f, top = 400f, right = 200f, bottom = 450f)

    @Test
    fun `loading to result resize on the same side keeps the native menu stable`() {
        val loadingBounds = Rect(left = 80f, top = 474f, right = 220f, bottom = 550f)
        val resultBounds = Rect(left = 40f, top = 474f, right = 260f, bottom = 700f)

        requiresActionModeReposition(selectionBounds, loadingBounds, resultBounds) shouldBe false
    }

    @Test
    fun `popup crossing the selection repositions the native menu`() {
        val belowBounds = Rect(left = 40f, top = 474f, right = 260f, bottom = 700f)
        val aboveBounds = Rect(left = 40f, top = 150f, right = 260f, bottom = 376f)

        requiresActionModeReposition(selectionBounds, belowBounds, aboveBounds) shouldBe true
    }

    @Test
    fun `popup participation changes reposition the native menu`() {
        val popupBounds = Rect(left = 40f, top = 474f, right = 260f, bottom = 700f)

        requiresActionModeReposition(selectionBounds, null, popupBounds) shouldBe true
        requiresActionModeReposition(selectionBounds, popupBounds, null) shouldBe true
    }
}
