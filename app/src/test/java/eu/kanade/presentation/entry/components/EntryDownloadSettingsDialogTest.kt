package eu.kanade.presentation.entry.components

import io.kotest.matchers.shouldBe
import mihon.entry.interactions.EntryDownloadOption
import mihon.entry.interactions.EntryDownloadOptionGroup
import mihon.entry.interactions.EntryDownloadOptions
import org.junit.jupiter.api.Test

class EntryDownloadSettingsDialogTest {

    @Test
    fun `download selection requires loaded options`() {
        isEntryDownloadSelectionValid(options = null, selections = emptyMap()) shouldBe false
    }

    @Test
    fun `download selection without dub options can use automatic dub`() {
        isEntryDownloadSelectionValid(options(), selections = emptyMap()) shouldBe true
    }

    @Test
    fun `download selection with dub options requires explicit dub`() {
        val options = options(dubOptions = listOf(EntryDownloadOption("en", "English")))

        isEntryDownloadSelectionValid(options, selections = emptyMap()) shouldBe false
        isEntryDownloadSelectionValid(options, selections = mapOf("dub" to "")) shouldBe false
        isEntryDownloadSelectionValid(options, selections = mapOf("dub" to "en")) shouldBe true
    }

    private fun options(
        dubOptions: List<EntryDownloadOption> = emptyList(),
    ): EntryDownloadOptions {
        return EntryDownloadOptions(
            groups = listOf(
                EntryDownloadOptionGroup(
                    key = "dub",
                    label = "Dub",
                    options = dubOptions,
                    required = dubOptions.isNotEmpty(),
                ),
            ),
        )
    }
}
