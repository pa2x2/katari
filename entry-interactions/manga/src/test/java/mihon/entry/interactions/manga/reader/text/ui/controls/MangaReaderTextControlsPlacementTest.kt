package mihon.entry.interactions.manga.reader.text.ui.controls

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MangaReaderTextControlsPlacementTest {

    private val area = IntRect(left = 0, top = 100, right = 1000, bottom = 2100)
    private val toolbar = IntSize(width = 800, height = 100)

    @Test
    fun `a toolbar dropped inside the reader keeps its place when the reader changes size`() {
        val placement = settledPlacement(IntOffset(100, 1050), toolbar, area)

        placement.docked shouldBe null
        placement.offsetIn(area, toolbar) shouldBe IntOffset(100, 1050)
        placement.offsetIn(IntRect(0, 100, 1400, 1100), toolbar) shouldBe IntOffset(300, 550)
    }

    @Test
    fun `a toolbar pushed past a side edge is tucked into it at the height it was dropped`() {
        settledPlacement(IntOffset(-300, 1050), toolbar, area) shouldBe
            MangaReaderTextControlsPlacement(0f, 0.5f, MangaReaderTextControlsEdge.Left)
        settledPlacement(IntOffset(500, 100), toolbar, area) shouldBe
            MangaReaderTextControlsPlacement(1f, 0f, MangaReaderTextControlsEdge.Right)
    }

    @Test
    fun `a toolbar nudged slightly past an edge stays shown`() {
        settledPlacement(IntOffset(-150, 100), toolbar, area).docked shouldBe null
    }

    @Test
    fun `a tucked toolbar comes back beside the edge it was tucked into`() {
        MangaReaderTextControlsPlacement(0.5f, 0.3f, MangaReaderTextControlsEdge.Right).expanded() shouldBe
            MangaReaderTextControlsPlacement(1f, 0.3f)
    }
}
