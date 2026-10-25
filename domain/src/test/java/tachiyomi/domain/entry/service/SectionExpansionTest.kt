package tachiyomi.domain.entry.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SectionExpansionTest {

    @Test
    fun `toggles show before they are stored and survive the stored state catching up one write at a time`() {
        val current = 1L
        val other = 2L
        var expansion = SectionExpansion<Long>()
        expansion.shown(current) shouldBe setOf(current)

        // Collapsing the current member keeps it collapsed even though nothing is stored for it.
        expansion = expansion.toggle(current, current).withSaved(emptySet())
        expansion.shown(current) shouldBe emptySet()

        // A quick double tap ends collapsed while the first write's result arrives before the second's.
        expansion = expansion.toggle(other, current)
        expansion.shown(current) shouldBe setOf(other)
        expansion = expansion.toggle(other, current)
        expansion.shown(current) shouldBe emptySet()
        expansion = expansion.withSaved(setOf(other))
        expansion.shown(current) shouldBe emptySet()
        expansion = expansion.withSaved(emptySet())
        expansion.shown(current) shouldBe emptySet()

        // A choice stored elsewhere, such as from the player, is followed once nothing here is waiting to be stored.
        expansion = expansion.withSaved(setOf(other))
        expansion.shown(current) shouldBe setOf(other)
    }
}
