package mihon.text.recognition.provider.onnx.paddle.lines

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test

class CharacterColumnsTest {

    private fun box(left: Int, top: Int, right: Int, bottom: Int) =
        RotatedRectangle.axisAligned(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

    private fun List<RotatedRectangle>.boundsList() = map { it.bounds().map(Double::toInt) }

    @Test
    fun `separately detected characters of each column are joined, and neighbouring columns stay apart`() {
        val right = listOf(box(100, 0, 130, 30), box(101, 40, 129, 70), box(100, 80, 130, 110))
        val left = listOf(box(40, 0, 70, 30), box(41, 42, 70, 72))

        stackCharacterColumns(right + left).boundsList() shouldContainExactlyInAnyOrder listOf(
            listOf(100, 0, 130, 110),
            listOf(40, 0, 70, 72),
        )
    }
}
