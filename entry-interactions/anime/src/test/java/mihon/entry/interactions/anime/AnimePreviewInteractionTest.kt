package mihon.entry.interactions.anime

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryPreviewImage
import eu.kanade.tachiyomi.source.entry.EntryPreviewSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryPreviewPageStatus
import mihon.entry.interactions.settings.EntryInteractionPreferences
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

class AnimePreviewInteractionTest {
    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `preview support and config require anime preview source`() {
        val source = previewSource()
        val preferences = EntryInteractionPreferences(InMemoryPreferenceStore()).apply {
            enableAnimePreview.set(true)
        }
        val interaction = AnimePreviewInteraction(preferences, sourceManager(source))

        interaction.isSupported(entry()) shouldBe true
        interaction.config(entry()).enabled shouldBe true
        interaction.isSupported(entry(type = EntryType.MANGA)) shouldBe false

        val unsupportedInteraction = AnimePreviewInteraction(
            preferences,
            sourceManager(mockk<UnifiedSource>()),
        )
        unsupportedInteraction.isSupported(entry()) shouldBe false
        unsupportedInteraction.config(entry()).enabled shouldBe false
    }

    @Test
    fun `loads source title images as ready preview pages and honors page count`() = runTest {
        val source = previewSource(
            listOf(
                EntryPreviewImage(index = 2, imageUrl = "https://example.org/two.jpg"),
                EntryPreviewImage(index = 5, imageUrl = "https://example.org/five.jpg"),
            ),
        )
        val interaction = AnimePreviewInteraction(
            EntryInteractionPreferences(InMemoryPreferenceStore()),
            sourceManager(source),
        )
        val entry = entry(title = "Requested anime")
        val handle = interaction.loadPreview(context, entry, chapter = null, source, pageCount = 1)

        handle.entryType shouldBe EntryType.ANIME
        handle.chapterId shouldBe null
        handle.pages.map { it.index } shouldContainExactly listOf(2)
        handle.pages.map { it.imageModel } shouldContainExactly listOf("https://example.org/two.jpg")
        handle.pages.single().status.value shouldBe EntryPreviewPageStatus.Ready
        handle.pages.single().progress.value shouldBe 100
        handle.pages.single().canOpen shouldBe false
        coVerify(exactly = 1) {
            source.getEntryPreview(match { it.title == "Requested anime" && it.type == EntryType.ANIME })
        }
    }

    private fun sourceManager(source: UnifiedSource): SourceManager = mockk {
        every { this@mockk.get(any()) } returns source
    }

    private fun previewSource(images: List<EntryPreviewImage> = emptyList()): EntryPreviewSource = mockk {
        every { id } returns 1L
        every { name } returns "Preview source"
        coEvery { getEntryPreview(any()) } returns images
    }

    private fun entry(
        type: EntryType = EntryType.ANIME,
        title: String = "Anime",
    ): Entry = Entry.create().copy(
        id = 1L,
        source = 1L,
        title = title,
        type = type,
    )
}
