package mihon.entry.interactions.settings

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingsCategory
import mihon.entry.viewer.settings.ViewerSettingsProvider
import org.junit.jupiter.api.Test

class DefaultViewerSettingsInteractionTest {

    @Test
    fun `providers can be added and removed observably`() {
        val manga = provider("manga", "Manga")
        val epub = provider("epub", "EPUB")
        val interaction = DefaultViewerSettingsInteraction(listOf(manga))

        interaction.register(epub)
        interaction.providers.value.map { it.id }.shouldContainExactly("epub", "manga")
        interaction.provider("epub") shouldBe epub

        interaction.unregister("manga")
        interaction.providers.value.map { it.id }.shouldContainExactly("epub")
        interaction.provider("manga") shouldBe null
    }

    @Test
    fun `duplicate provider IDs are rejected`() {
        val interaction = DefaultViewerSettingsInteraction(listOf(provider("same", "First")))

        shouldThrow<IllegalStateException> {
            interaction.register(provider("same", "Second"))
        }
    }

    private fun provider(id: String, name: String): ViewerSettingsProvider {
        return object : ViewerSettingsProvider {
            override val id = id
            override val category = ViewerSettingsCategory.READER
            override val displayName = name
            override val settings = emptyList<ViewerSettingDefinition<*>>()
        }
    }
}
