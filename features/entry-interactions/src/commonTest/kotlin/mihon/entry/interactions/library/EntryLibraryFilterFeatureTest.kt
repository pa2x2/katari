package mihon.entry.interactions.library

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.TriState

class EntryLibraryFilterFeatureTest {
    @Test
    fun `active progress predicates exclude unknown state for both polarities`() {
        val feature = DefaultEntryLibraryFilterFeature(
            EntryLibraryFilterTypes(
                progress = setOf(EntryType.BOOK),
                bookmark = emptySet(),
                releasePeriod = emptySet(),
            ),
        )
        val targets = arrayOf(
            target(EntryType.BOOK, unconsumed = true),
            target(EntryType.ANIME, unconsumed = null, started = null),
        )

        feature.filter(request(*targets, policy = policy(unconsumed = TriState.ENABLED_IS)))
            .includedTargetIndices.shouldContainExactly(0)
        feature.filter(request(*targets, policy = policy(unconsumed = TriState.ENABLED_NOT)))
            .includedTargetIndices.shouldContainExactly()
    }

    private fun request(
        vararg targets: EntryLibraryFilterTarget,
        policy: EntryLibraryFilterPolicy = policy(),
    ): EntryLibraryFilterRequest {
        return EntryLibraryFilterRequest(targets.toList(), policy)
    }

    private fun target(
        type: EntryType,
        downloaded: Boolean = false,
        unconsumed: Boolean? = false,
        started: Boolean? = false,
        bookmarked: Boolean? = false,
        completed: Boolean = false,
        outsideReleasePeriod: Boolean = false,
        trackers: Set<Long> = emptySet(),
    ): EntryLibraryFilterTarget {
        return EntryLibraryFilterTarget(
            type = type,
            isDownloadedOrLocal = downloaded,
            hasUnconsumed = unconsumed,
            hasStarted = started,
            hasBookmarks = bookmarked,
            isCompleted = completed,
            isOutsideReleasePeriod = outsideReleasePeriod,
            trackerIds = trackers,
        )
    }

    private fun policy(
        downloadedOnly: Boolean = false,
        downloaded: TriState = TriState.DISABLED,
        unconsumed: TriState = TriState.DISABLED,
        notStarted: TriState = TriState.DISABLED,
        bookmarked: TriState = TriState.DISABLED,
        completed: TriState = TriState.DISABLED,
        outsideReleasePeriod: TriState = TriState.DISABLED,
        outsideReleasePeriodEnabled: Boolean = false,
        tracking: Map<Long, TriState> = emptyMap(),
    ): EntryLibraryFilterPolicy {
        return EntryLibraryFilterPolicy(
            downloadedOnly = downloadedOnly,
            downloaded = downloaded,
            unconsumed = unconsumed,
            notStarted = notStarted,
            bookmarked = bookmarked,
            completed = completed,
            outsideReleasePeriod = outsideReleasePeriod,
            outsideReleasePeriodEnabled = outsideReleasePeriodEnabled,
            tracking = tracking,
        )
    }
}
