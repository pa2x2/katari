package mihon.entry.interactions.book.document.reader

import androidx.compose.ui.geometry.Rect
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class BookSelectionActionModeAvoidanceTest {
    private val selectionBounds = Rect(left = 100f, top = 400f, right = 200f, bottom = 450f)
    private val viewport = Rect(left = 0f, top = 0f, right = 1080f, bottom = 2400f)
    private val toolbarHeightPx = 189f

    @Test
    fun `loading to result resize on the same side keeps the native menu stable`() {
        val loadingBounds = Rect(left = 80f, top = 474f, right = 220f, bottom = 550f)
        val resultBounds = Rect(left = 40f, top = 474f, right = 260f, bottom = 700f)

        requiresActionModeReposition(
            selectionBounds,
            loadingBounds,
            resultBounds,
            viewport,
            toolbarHeightPx,
        ) shouldBe false
    }

    @Test
    fun `popup below growing near the viewport top repositions the native menu`() {
        val selection = Rect(left = 100f, top = 80f, right = 200f, bottom = 130f)
        val loadingBounds = Rect(left = 80f, top = 154f, right = 220f, bottom = 250f)
        val resultBounds = Rect(left = 40f, top = 154f, right = 260f, bottom = 400f)

        requiresActionModeReposition(
            selection,
            loadingBounds,
            resultBounds,
            viewport,
            toolbarHeightPx,
        ) shouldBe true
    }

    @Test
    fun `popup above growing near the viewport bottom repositions the native menu`() {
        val selection = Rect(left = 100f, top = 1800f, right = 200f, bottom = 1850f)
        val loadingBounds = Rect(left = 80f, top = 1650f, right = 220f, bottom = 1776f)
        val resultBounds = Rect(left = 40f, top = 1500f, right = 260f, bottom = 1776f)

        requiresActionModeReposition(
            selection,
            loadingBounds,
            resultBounds,
            viewport,
            toolbarHeightPx,
        ) shouldBe true
    }

    @Test
    fun `popup shrink on the same side keeps the native menu stable`() {
        val selection = Rect(left = 100f, top = 80f, right = 200f, bottom = 130f)
        val loadingBounds = Rect(left = 40f, top = 154f, right = 260f, bottom = 400f)
        val resultBounds = Rect(left = 80f, top = 154f, right = 220f, bottom = 250f)

        requiresActionModeReposition(
            selection,
            loadingBounds,
            resultBounds,
            viewport,
            toolbarHeightPx,
        ) shouldBe false
    }

    @Test
    fun `popup crossing the selection repositions the native menu`() {
        val belowBounds = Rect(left = 40f, top = 474f, right = 260f, bottom = 700f)
        val aboveBounds = Rect(left = 40f, top = 150f, right = 260f, bottom = 376f)

        requiresActionModeReposition(
            selectionBounds,
            belowBounds,
            aboveBounds,
            viewport,
            toolbarHeightPx,
        ) shouldBe true
    }

    @Test
    fun `popup appearing away from the native menu keeps it stable`() {
        val popupBounds = Rect(left = 40f, top = 474f, right = 260f, bottom = 700f)

        requiresActionModeReposition(
            selectionBounds,
            null,
            popupBounds,
            viewport,
            toolbarHeightPx,
        ) shouldBe false
        requiresActionModeReposition(
            selectionBounds,
            popupBounds,
            null,
            viewport,
            toolbarHeightPx,
        ) shouldBe false
    }

    @Test
    fun `popup appearing towards the native menu repositions it`() {
        val selection = Rect(left = 100f, top = 80f, right = 200f, bottom = 130f)
        val popupBounds = Rect(left = 40f, top = 154f, right = 260f, bottom = 400f)

        requiresActionModeReposition(
            selection,
            null,
            popupBounds,
            viewport,
            toolbarHeightPx,
        ) shouldBe true
        requiresActionModeReposition(
            selection,
            popupBounds,
            null,
            viewport,
            toolbarHeightPx,
        ) shouldBe true
    }

    @Test
    fun `popup growing into the selection without moving the union keeps the native menu stable`() {
        val selection = Rect(left = 369f, top = 965f, right = 382f, bottom = 1126f)
        val loadingBounds = Rect(left = 36f, top = 730f, right = 837f, bottom = 911f)
        val resultBounds = Rect(left = 36f, top = 730f, right = 837f, bottom = 1007f)

        requiresActionModeReposition(
            selection,
            loadingBounds,
            resultBounds,
            viewport,
            toolbarHeightPx,
        ) shouldBe false
    }

    @Test
    fun `popup width growth without vertical change keeps the native menu stable`() {
        val loadingBounds = Rect(left = 80f, top = 474f, right = 220f, bottom = 550f)
        val resultBounds = Rect(left = 40f, top = 474f, right = 260f, bottom = 550f)

        requiresActionModeReposition(
            selectionBounds,
            loadingBounds,
            resultBounds,
            viewport,
            toolbarHeightPx,
        ) shouldBe false
    }
}
