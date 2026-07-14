package eu.kanade.presentation.browse

import eu.kanade.presentation.browse.immersive.EntryImmersivePositionState
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CatalogContentTest {

    @Test
    fun `shared immersive position state tracks the latest non-negative item`() {
        val state = EntryImmersivePositionState(initialItemIndex = 4)

        state.updateItemIndex(9)
        state.itemIndex shouldBe 9

        state.updateItemIndex(-1)
        state.itemIndex shouldBe 0
    }

    @Test
    fun `catalog position maps directly when there is no prepend row`() {
        catalogLazyIndex(catalogItemIndex = 7, prependItemCount = 0, itemCount = 20) shouldBe 7
        catalogItemIndex(lazyItemIndex = 7, prependItemCount = 0, itemCount = 20) shouldBe 7
    }

    @Test
    fun `catalog position ignores the prepend loading row`() {
        catalogLazyIndex(catalogItemIndex = 7, prependItemCount = 1, itemCount = 20) shouldBe 8
        catalogItemIndex(lazyItemIndex = 8, prependItemCount = 1, itemCount = 20) shouldBe 7
    }

    @Test
    fun `catalog position cannot resolve to the append loading row`() {
        catalogItemIndex(lazyItemIndex = 20, prependItemCount = 0, itemCount = 20) shouldBe 19
    }
}
