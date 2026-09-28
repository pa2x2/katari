package tachiyomi.core.common.util.system

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TallImageSplitCalculatorTest {

    @Test
    fun `tall image is split only when it needs more than one part`() {
        assertFalse(
            TallImageSplitCalculator.shouldSplit(
                imageWidth = 1024,
                imageHeight = 4385,
                optimalImageHeight = 4386,
            ),
        )
        assertTrue(
            TallImageSplitCalculator.shouldSplit(
                imageWidth = 1024,
                imageHeight = 4385,
                optimalImageHeight = 4384,
            ),
        )
    }
}
