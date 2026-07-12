package eu.kanade.tachiyomi.ui.entry

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

class EntrySelectionActionLabelsTest {

    @Test
    fun `empty selection uses consumed labels`() {
        val labels = emptyList<EntryType>().entrySelectionActionLabels()

        labels.markAsReadLabel shouldBe MR.strings.action_mark_as_consumed
        labels.markAsUnreadLabel shouldBe MR.strings.action_mark_as_unconsumed
    }

    @Test
    fun `anime-only selection uses watched labels`() {
        val labels = listOf(EntryType.ANIME).entrySelectionActionLabels()

        labels.markAsReadLabel shouldBe MR.strings.action_mark_as_watched
        labels.markAsUnreadLabel shouldBe MR.strings.action_mark_as_unwatched
    }

    @Test
    fun `manga-only selection uses read labels`() {
        val labels = listOf(EntryType.MANGA).entrySelectionActionLabels()

        labels.markAsReadLabel shouldBe MR.strings.action_mark_as_read
        labels.markAsUnreadLabel shouldBe MR.strings.action_mark_as_unread
    }

    @Test
    fun `mixed selection uses consumed labels`() {
        val labels = listOf(EntryType.MANGA, EntryType.ANIME).entrySelectionActionLabels()

        labels.markAsReadLabel shouldBe MR.strings.action_mark_as_consumed
        labels.markAsUnreadLabel shouldBe MR.strings.action_mark_as_unconsumed
    }
}
