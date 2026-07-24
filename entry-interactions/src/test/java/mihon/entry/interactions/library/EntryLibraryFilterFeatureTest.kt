package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryLibraryFilterFeatureTest {
    @Test
    fun `provider-less type receives generic policy and behavior contract`() {
        val composition = composition(plugin(EntryType.ANIME))
        val feature = DefaultEntryLibraryFilterFeature(composition.featureGraphEvaluation)

        val result = feature.filter(
            request(
                target(EntryType.ANIME, downloaded = false),
                policy = policy(downloaded = TriState.ENABLED_NOT),
            ),
        )

        result.includedTargetIndices.shouldContainExactly(0)
        result.hasActiveFilters.shouldBeTrue()
        result.availability.progressSummary.isAvailable.shouldBeFalse()
        result.availability.bookmarking.isAvailable.shouldBeFalse()
        result.availability.outsideReleasePeriod.isAvailable.shouldBeFalse()
    }

    @Test
    fun `capability controls derive availability from current library types`() {
        val composition = composition(
            plugin(
                EntryType.BOOK,
                EntryLibraryProgressCapability.bind(LibraryProgressProvider()),
                EntryBookmarkCapability.bind(BookmarkProcessor()),
                EntryOutsideReleasePeriodFilterCapability.bind(OutsideReleasePeriodProvider()),
            ),
            plugin(EntryType.ANIME),
        )
        val result = DefaultEntryLibraryFilterFeature(composition.featureGraphEvaluation).filter(
            request(
                target(EntryType.BOOK),
                target(EntryType.ANIME),
            ),
        )

        result.availability.bookmarking.applicableTypes shouldBe setOf(EntryType.BOOK)
        result.availability.bookmarking.inapplicableTypes shouldBe setOf(EntryType.ANIME)
        result.availability.outsideReleasePeriod.applicableTypes shouldBe setOf(EntryType.BOOK)
        result.availability.outsideReleasePeriod.inapplicableTypes shouldBe setOf(EntryType.ANIME)
    }

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
    fun `shared policy owns downloaded progress status and tracker interpretation`() {
        val feature = DefaultEntryLibraryFilterFeature(
            composition(plugin(EntryType.ANIME)).featureGraphEvaluation,
        )
        val targets = listOf(
            target(
                EntryType.ANIME,
                downloaded = true,
                unconsumed = true,
                started = true,
                completed = true,
                trackers = setOf(1L),
            ),
            target(
                EntryType.ANIME,
                downloaded = false,
                unconsumed = false,
                started = false,
                completed = false,
                trackers = setOf(2L),
            ),
        )
        val result = feature.filter(
            EntryLibraryFilterRequest(
                targets = targets,
                policy = policy(
                    downloadedOnly = true,
                    downloaded = TriState.ENABLED_NOT,
                    unconsumed = TriState.ENABLED_IS,
                    notStarted = TriState.ENABLED_NOT,
                    completed = TriState.ENABLED_IS,
                    tracking = mapOf(1L to TriState.ENABLED_IS, 2L to TriState.ENABLED_NOT),
                ),
            ),
        )

        result.includedTargetIndices.shouldContainExactly(0)
        result.hasActiveFilters.shouldBeTrue()
    }

    @Test
    fun `aggregate target state is interpreted without merge-specific policy`() {
        val feature = DefaultEntryLibraryFilterFeature(
            composition(
                plugin(
                    EntryType.BOOK,
                    EntryLibraryProgressCapability.bind(LibraryProgressProvider()),
                    EntryBookmarkCapability.bind(BookmarkProcessor()),
                ),
            ).featureGraphEvaluation,
        )
        val aggregate = target(
            EntryType.BOOK,
            downloaded = true,
            unconsumed = true,
            started = true,
            bookmarked = true,
        )

        val result = feature.filter(
            request(
                aggregate,
                policy = policy(
                    downloaded = TriState.ENABLED_IS,
                    unconsumed = TriState.ENABLED_IS,
                    bookmarked = TriState.ENABLED_IS,
                ),
            ),
        )

        result.includedTargetIndices.shouldContainExactly(0)
    }

    @Test
    fun `uncomposed target fails instead of becoming unsupported`() {
        val feature = DefaultEntryLibraryFilterFeature(
            composition(plugin(EntryType.BOOK)).featureGraphEvaluation,
        )

        shouldThrow<IllegalStateException> {
            feature.filter(request(target(EntryType.ANIME)))
        }
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
