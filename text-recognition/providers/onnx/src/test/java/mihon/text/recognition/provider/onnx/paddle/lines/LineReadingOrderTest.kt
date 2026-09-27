package mihon.text.recognition.provider.onnx.paddle.lines

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class LineReadingOrderTest {

    private val lines = mapOf(
        "a" to doubleArrayOf(0.0, 0.0, 50.0, 20.0),
        "b" to doubleArrayOf(60.0, 2.0, 110.0, 22.0),
        "c" to doubleArrayOf(0.0, 30.0, 50.0, 50.0),
    )

    @Test
    fun `horizontal lines are read row by row and from left to right within a row`() {
        linesInReadingOrder(listOf("c", "b", "a"), lines::getValue, vertical = false) shouldContainExactly
            listOf("a", "b", "c")
    }

    @Test
    fun `vertical columns are read from right to left`() {
        val columns = mapOf(
            "left" to doubleArrayOf(0.0, 0.0, 20.0, 100.0),
            "right" to doubleArrayOf(40.0, 10.0, 60.0, 90.0),
        )

        linesInReadingOrder(listOf("left", "right"), columns::getValue, vertical = true) shouldContainExactly
            listOf("right", "left")
    }
}
