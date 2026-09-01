package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HomeNavigationOverflowPositionProviderTest {

    @Test
    fun `menu is end aligned immediately above its navigation anchor`() {
        val provider = HomeNavigationOverflowPositionProvider(gap = 8, windowMargin = 8)

        provider.calculatePosition(
            anchorBounds = IntRect(left = 380, top = 700, right = 480, bottom = 780),
            windowSize = IntSize(width = 500, height = 800),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 200, height = 120),
        ) shouldBe IntOffset(x = 280, y = 572)
    }

    @Test
    fun `menu stays within window margins when its anchor is at an edge`() {
        val provider = HomeNavigationOverflowPositionProvider(gap = 8, windowMargin = 8)

        provider.calculatePosition(
            anchorBounds = IntRect(left = 0, top = 4, right = 100, bottom = 84),
            windowSize = IntSize(width = 500, height = 800),
            layoutDirection = LayoutDirection.Rtl,
            popupContentSize = IntSize(width = 200, height = 120),
        ) shouldBe IntOffset(x = 8, y = 92)
    }
}
