package mihon.entry.interactions.book.document.reader

import androidx.compose.ui.geometry.Rect
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class BookSelectionActionModeAvoidanceTest {
    private val selectionBounds = Rect(left = 100f, top = 400f, right = 200f, bottom = 450f)

    @Test
    fun `popup growth below the selection repositions the native menu`() {
        val loadingBounds = Rect(left = 80f, top = 474f, right = 220f, bottom = 550f)
        val resultBounds = Rect(left = 80f, top = 474f, right = 220f, bottom = 700f)

        requiresActionModeReposition(selectionBounds, loadingBounds, resultBounds) shouldBe true
    }

    @Test
    fun `popup growth above the selection repositions the native menu`() {
        val loadingBounds = Rect(left = 80f, top = 250f, right = 220f, bottom = 376f)
        val resultBounds = Rect(left = 80f, top = 150f, right = 220f, bottom = 376f)

        requiresActionModeReposition(selectionBounds, loadingBounds, resultBounds) shouldBe true
    }

    @Test
    fun `popup growth near the viewport top repositions the native menu`() {
        val selection = Rect(left = 100f, top = 80f, right = 200f, bottom = 130f)
        val loadingBounds = Rect(left = 80f, top = 154f, right = 220f, bottom = 250f)
        val resultBounds = Rect(left = 80f, top = 154f, right = 220f, bottom = 400f)

        requiresActionModeReposition(selection, loadingBounds, resultBounds) shouldBe true
    }

    @Test
    fun `popup shrink keeps the native menu stable`() {
        val loadingBounds = Rect(left = 80f, top = 474f, right = 220f, bottom = 700f)
        val resultBounds = Rect(left = 80f, top = 474f, right = 220f, bottom = 550f)

        requiresActionModeReposition(selectionBounds, loadingBounds, resultBounds) shouldBe false
    }

    @Test
    fun `popup crossing the selection repositions the native menu`() {
        val belowBounds = Rect(left = 40f, top = 474f, right = 260f, bottom = 700f)
        val aboveBounds = Rect(left = 40f, top = 150f, right = 260f, bottom = 376f)

        requiresActionModeReposition(selectionBounds, belowBounds, aboveBounds) shouldBe true
    }

    @Test
    fun `popup appearance repositions the native menu`() {
        val popupBounds = Rect(left = 40f, top = 474f, right = 260f, bottom = 700f)

        requiresActionModeReposition(selectionBounds, null, popupBounds) shouldBe true
    }

    @Test
    fun `popup dismissal repositions the native menu`() {
        val popupBounds = Rect(left = 40f, top = 474f, right = 260f, bottom = 700f)

        requiresActionModeReposition(selectionBounds, popupBounds, null) shouldBe true
    }

    @Test
    fun `popup growing into the selection without moving the union keeps the native menu stable`() {
        val selection = Rect(left = 369f, top = 965f, right = 382f, bottom = 1126f)
        val loadingBounds = Rect(left = 36f, top = 730f, right = 837f, bottom = 911f)
        val resultBounds = Rect(left = 36f, top = 730f, right = 837f, bottom = 1007f)

        requiresActionModeReposition(selection, loadingBounds, resultBounds) shouldBe false
    }

    @Test
    fun `popup width growth without vertical change keeps the native menu stable`() {
        val loadingBounds = Rect(left = 80f, top = 474f, right = 220f, bottom = 550f)
        val resultBounds = Rect(left = 40f, top = 474f, right = 260f, bottom = 550f)

        requiresActionModeReposition(selectionBounds, loadingBounds, resultBounds) shouldBe false
    }

    @Test
    fun `unchanged popup bounds keep the native menu stable`() {
        val popupBounds = Rect(left = 40f, top = 474f, right = 260f, bottom = 700f)

        requiresActionModeReposition(selectionBounds, popupBounds, popupBounds) shouldBe false
        requiresActionModeReposition(selectionBounds, null, null) shouldBe false
    }
}
