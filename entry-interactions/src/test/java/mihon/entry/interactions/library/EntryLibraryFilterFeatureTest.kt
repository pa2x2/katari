package mihon.entry.interactions.library

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContainExactly
import mihon.entry.interactions.runtime.EntryInteractionComposition
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.EntryOutsideReleasePeriodFilterCapability
import mihon.entry.interactions.runtime.EntryOutsideReleasePeriodFilterProvider
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.entry.interactions.state.EntryBookmarkCapability
import mihon.entry.interactions.state.EntryBookmarkProcessor
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryLibraryFilterFeatureTest {
    @Test
    fun `bookmark state filters mixed supported and unsupported targets while release filtering passes unsupported`() {
        val composition = composition(
            plugin(
                EntryType.BOOK,
                EntryLibraryProgressCapability.bind(LibraryProgressProvider()),
                EntryBookmarkCapability.bind(BookmarkProcessor()),
                EntryOutsideReleasePeriodFilterCapability.bind(OutsideReleasePeriodProvider()),
            ),
            plugin(EntryType.ANIME),
        )
        val feature = DefaultEntryLibraryFilterFeature(composition.featureGraphEvaluation)
        val bookmarked = feature.filter(
            request(
                target(EntryType.BOOK, bookmarked = true),
                target(EntryType.ANIME, bookmarked = false),
                policy = policy(bookmarked = TriState.ENABLED_IS),
            ),
        )
        val releasePeriod = feature.filter(
            request(
                target(EntryType.BOOK, outsideReleasePeriod = false),
                target(EntryType.ANIME, outsideReleasePeriod = false),
                policy = policy(
                    outsideReleasePeriod = TriState.ENABLED_IS,
                    outsideReleasePeriodEnabled = true,
                ),
            ),
        )

        bookmarked.includedTargetIndices.shouldContainExactly(0)
        releasePeriod.includedTargetIndices.shouldContainExactly(1)
    }

    @Test
    fun `unavailable capability filters neither filter nor report active`() {
        val feature = DefaultEntryLibraryFilterFeature(
            composition(
                plugin(
                    EntryType.ANIME,
                    EntryLibraryProgressCapability.bind(LibraryProgressProvider(EntryType.ANIME)),
                ),
            ).featureGraphEvaluation,
        )
        val result = feature.filter(
            request(
                target(EntryType.ANIME),
                policy = policy(
                    bookmarked = TriState.ENABLED_IS,
                    outsideReleasePeriod = TriState.ENABLED_IS,
                    outsideReleasePeriodEnabled = true,
                ),
            ),
        )

        result.includedTargetIndices.shouldContainExactly(0)
        result.hasActiveFilters.shouldBeFalse()
    }

    @Test
    fun `active progress predicates exclude unknown state for both polarities`() {
        val feature = DefaultEntryLibraryFilterFeature(
            composition(
                plugin(
                    EntryType.BOOK,
                    EntryLibraryProgressCapability.bind(LibraryProgressProvider()),
                ),
                plugin(EntryType.ANIME),
            ).featureGraphEvaluation,
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

    private fun composition(vararg plugins: EntryInteractionPlugin): EntryInteractionComposition {
        return createEntryInteractionComposition(
            plugins = plugins.toList(),
            featureContributors = listOf(EntryLibraryFilterFeatureContributor),
        )
    }

    private fun plugin(
        type: EntryType,
        vararg bindings: EntryInteractionProviderBinding<*>,
    ): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = type
            override val owner = ContributionOwner("test.type.${type.name.lowercase()}")
            override val providerBindings = bindings.toList()
        }
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

    private class BookmarkProcessor : EntryBookmarkProcessor {
        override val type = EntryType.BOOK

        override suspend fun setBookmarked(
            entry: Entry,
            chapters: List<EntryChapter>,
            bookmarked: Boolean,
        ) = Unit
    }

    private class OutsideReleasePeriodProvider : EntryOutsideReleasePeriodFilterProvider {
        override val type = EntryType.BOOK
    }

    private class LibraryProgressProvider(
        override val type: EntryType = EntryType.BOOK,
    ) : EntryLibraryProgressProvider {
        override suspend fun evidence(entry: Entry, chapters: List<EntryChapter>) =
            EntryLibraryProgressEvidence(false, null, null, 0L)
    }
}
