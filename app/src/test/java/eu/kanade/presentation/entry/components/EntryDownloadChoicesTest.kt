package eu.kanade.presentation.entry.components

import io.kotest.matchers.shouldBe
import mihon.entry.interactions.download.EntryDownloadOption
import mihon.entry.interactions.download.EntryDownloadOptionGroup
import mihon.entry.interactions.download.EntryDownloadOptions
import org.junit.jupiter.api.Test

class EntryDownloadChoicesTest {

    @Test
    fun `switching the server drops a stream picked from the old server's list and keeps what the new one offers`() {
        val switching = listing(server = "a", streams = listOf("a1", "a2")).toChoices()
            .pick(STREAM, "a2")
            .pick(SUBTITLE, "en")
            .pick(SERVER, "b")

        // The stream saved for the title, from the old server, is the new listing's default, and it isn't offered.
        val switched = switching.reloaded(listing(server = "b", streams = listOf("b1"), savedStream = "a1"))

        switching.reload?.picks shouldBe mapOf(SERVER to "b")
        switched.selections shouldBe mapOf(SERVER to "b", STREAM to null, SUBTITLE to "en")
        switched.canDownload shouldBe true
    }

    @Test
    fun `a server whose options can't be listed can't be downloaded with the old server's stream`() {
        val failed = listing(server = "a", streams = listOf("a1")).toChoices()
            .pick(SERVER, "b")
            .reloaded(EntryDownloadOptions(groups = emptyList(), listingFailed = true))

        failed.shownGroups.map(EntryDownloadOptionGroup::key) shouldBe listOf(SERVER)
        failed.canDownload shouldBe false
        // Picking the server again lists it again.
        failed.pick(SERVER, "b").reload?.failed shouldBe false
    }

    private fun listing(server: String, streams: List<String>, savedStream: String? = null) = EntryDownloadOptions(
        groups = listOf(
            group(SERVER, listOf("a", "b"), selectedKey = server, reloadsOptions = true),
            group(STREAM, streams, selectedKey = savedStream),
            group(SUBTITLE, listOf("off", "en"), selectedKey = null),
        ),
    )

    private fun group(key: String, options: List<String>, selectedKey: String?, reloadsOptions: Boolean = false) =
        EntryDownloadOptionGroup(
            key = key,
            label = key,
            options = options.map { EntryDownloadOption(it, it) },
            selectedKey = selectedKey,
            defaultLabel = "Automatic",
            reloadsOptions = reloadsOptions,
        )

    private companion object {
        const val SERVER = "server"
        const val STREAM = "stream"
        const val SUBTITLE = "subtitle"
    }
}
