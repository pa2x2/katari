package eu.kanade.tachiyomi.ui.entry

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import mihon.entry.interactions.EntryTypePresentationFeature
import mihon.entry.interactions.EntryTypePresentationResult
import mihon.entry.interactions.genericEntryTypePresentation
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

class EntrySelectionActionLabelsTest {

    @Test
    fun `empty selection uses consumed labels`() {
        val feature = RecordingPresentationFeature()

        val labels = emptyList<EntryType>().entrySelectionActionLabels(feature)

        labels.markAsReadLabel shouldBe MR.strings.action_mark_as_consumed
        labels.markAsUnreadLabel shouldBe MR.strings.action_mark_as_unconsumed
        feature.requestedTypes shouldBe listOf<EntryType?>(null)
    }

    @Test
    fun `homogeneous selection uses contributed labels`() {
        val feature = RecordingPresentationFeature()

        val labels = listOf(EntryType.BOOK, EntryType.BOOK).entrySelectionActionLabels(feature)

        labels.markAsReadLabel shouldBe MR.strings.action_mark_as_read
        labels.markAsUnreadLabel shouldBe MR.strings.action_mark_as_unread
        feature.requestedTypes shouldBe listOf(EntryType.BOOK)
    }

    @Test
    fun `mixed selection uses consumed labels`() {
        val feature = RecordingPresentationFeature()

        val labels = listOf(EntryType.MANGA, EntryType.ANIME).entrySelectionActionLabels(feature)

        labels.markAsReadLabel shouldBe MR.strings.action_mark_as_consumed
        labels.markAsUnreadLabel shouldBe MR.strings.action_mark_as_unconsumed
        feature.requestedTypes shouldBe listOf<EntryType?>(null)
    }

    private class RecordingPresentationFeature : EntryTypePresentationFeature {
        override val genericPresentation = genericEntryTypePresentation
        val requestedTypes = mutableListOf<EntryType?>()

        private val contributedPresentation = genericPresentation.copy(
            markAsConsumedLabel = MR.strings.action_mark_as_read,
            markAsUnconsumedLabel = MR.strings.action_mark_as_unread,
        )

        override fun presentation(type: EntryType?): EntryTypePresentationResult {
            requestedTypes += type
            return if (type == null) {
                EntryTypePresentationResult.Generic(type, genericPresentation)
            } else {
                EntryTypePresentationResult.Contributed(type, contributedPresentation)
            }
        }
    }
}
