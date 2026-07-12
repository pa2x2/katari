package tachiyomi.domain.entry.interactor

import android.app.Application
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.entry.interactor.GetDuplicateLibraryEntries
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.DuplicateTitleExclusions

class GetEnhancedDuplicateLibraryEntriesTest {

    private val application = mockk<Application>()
    private val getDuplicateLibraryEntries = mockk<GetDuplicateLibraryEntries>()
    private val enhanceDuplicateLibraryEntries = mockk<EnhanceDuplicateLibraryEntries>()
    private val duplicatePreferences = mockk<DuplicatePreferences>()
    private val extendedEnabledPreference = MutablePreference(true)
    private val minimumMatchScorePreference = MutablePreference(DuplicatePreferences.DEFAULT_MINIMUM_MATCH_SCORE)
    private val coverWeightPreference = MutablePreference(DuplicatePreferences.DEFAULT_COVER_WEIGHT)
    private val titleExclusionPatternsPreference = MutablePreference(DuplicateTitleExclusions.defaultPatterns)

    private val interactor = GetEnhancedDuplicateLibraryEntries(
        application = application,
        getDuplicateLibraryEntries = getDuplicateLibraryEntries,
        enhanceDuplicateLibraryEntries = enhanceDuplicateLibraryEntries,
        duplicatePreferences = duplicatePreferences,
    )

    init {
        every { duplicatePreferences.extendedDuplicateDetectionEnabled } returns extendedEnabledPreference
        every { duplicatePreferences.minimumMatchScore } returns minimumMatchScorePreference
        every { duplicatePreferences.coverWeight } returns coverWeightPreference
        every { duplicatePreferences.titleExclusionPatterns } returns titleExclusionPatternsPreference
    }

    @Test
    fun `invoke enhances duplicate candidates`() = runTest {
        val entry = entry(1, "Frieren")
        val baseCandidates = listOf(candidate(2, 40, coverHashChecked = false))
        val enhancedCandidates = listOf(candidate(2, 54, coverHashChecked = true))

        coEvery { getDuplicateLibraryEntries(entry) } returns baseCandidates
        coEvery { enhanceDuplicateLibraryEntries(application, entry, baseCandidates) } returns enhancedCandidates

        interactor(entry) shouldBe enhancedCandidates

        coVerify(exactly = 1) { enhanceDuplicateLibraryEntries(application, entry, baseCandidates) }
    }

    @Test
    fun `invoke skips duplicate detection until entry metadata is initialized`() = runTest {
        val entry = entry(1, "Frieren", initialized = false)

        interactor(entry) shouldBe emptyList()

        coVerify(exactly = 0) { getDuplicateLibraryEntries(any()) }
        coVerify(exactly = 0) { enhanceDuplicateLibraryEntries(application, any(), any()) }
    }

    @Test
    fun `subscribe re-enhances when cover weight changes`() = runTest {
        val entry = entry(1, "Frieren")
        val baseCandidates = listOf(candidate(2, 40, coverHashChecked = false))
        val firstEnhancedCandidates = listOf(candidate(2, 54, coverHashChecked = true))
        val secondEnhancedCandidates = listOf(candidate(2, 44, coverHashChecked = true))
        val duplicateFlow = MutableStateFlow(baseCandidates)

        every {
            getDuplicateLibraryEntries.subscribe(any(), any())
        } returns duplicateFlow.asStateFlow()
        coEvery {
            enhanceDuplicateLibraryEntries(application, entry, baseCandidates)
        } returnsMany listOf(firstEnhancedCandidates, secondEnhancedCandidates)

        val subscriptionScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        try {
            val results = interactor.subscribe(flowOf(entry), subscriptionScope)
            val emissions = mutableListOf<List<DuplicateEntryCandidate>>()
            val job = subscriptionScope.launch(start = CoroutineStart.UNDISPATCHED) {
                results.take(3).toList(emissions)
            }

            testScheduler.advanceUntilIdle()
            coverWeightPreference.set(0)
            testScheduler.advanceUntilIdle()
            job.join()

            emissions shouldBe listOf(emptyList(), firstEnhancedCandidates, secondEnhancedCandidates)
            coVerify(exactly = 2) { enhanceDuplicateLibraryEntries(application, entry, baseCandidates) }
        } finally {
            subscriptionScope.cancel()
        }
    }

    @Test
    fun `title exclusions do not trigger enhancement config by themselves`() = runTest {
        val entry = entry(1, "Frieren")
        val baseCandidates = listOf(candidate(2, 40, coverHashChecked = false))
        val enhancedCandidates = listOf(candidate(2, 54, coverHashChecked = true))
        val duplicateFlow = MutableStateFlow(baseCandidates)

        every {
            getDuplicateLibraryEntries.subscribe(any(), any())
        } returns duplicateFlow.asStateFlow()
        coEvery {
            enhanceDuplicateLibraryEntries(application, entry, baseCandidates)
        } returns enhancedCandidates

        val subscriptionScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        try {
            val results = interactor.subscribe(flowOf(entry), subscriptionScope)
            val emissions = mutableListOf<List<DuplicateEntryCandidate>>()
            val job = subscriptionScope.launch(start = CoroutineStart.UNDISPATCHED) {
                results.take(2).toList(emissions)
            }

            testScheduler.advanceUntilIdle()
            titleExclusionPatternsPreference.set(listOf("[*]"))
            testScheduler.advanceUntilIdle()
            job.join()

            emissions shouldBe listOf(emptyList(), enhancedCandidates)
            coVerify(exactly = 1) { enhanceDuplicateLibraryEntries(application, entry, baseCandidates) }
        } finally {
            subscriptionScope.cancel()
        }
    }

    @Test
    fun `subscribe waits for metadata initialization before emitting duplicates`() = runTest {
        val uninitializedEntry = entry(1, "Frieren", initialized = false)
        val initializedEntry = uninitializedEntry.copy(initialized = true)
        val baseCandidates = listOf(candidate(2, 40, coverHashChecked = false))
        val enhancedCandidates = listOf(candidate(2, 54, coverHashChecked = true))
        val entryFlow = MutableStateFlow(uninitializedEntry)
        val duplicateFlow = MutableStateFlow(baseCandidates)

        every {
            getDuplicateLibraryEntries.subscribe(any(), any())
        } returns duplicateFlow.asStateFlow()
        coEvery {
            enhanceDuplicateLibraryEntries(application, initializedEntry, baseCandidates)
        } returns enhancedCandidates

        val subscriptionScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        try {
            val results = interactor.subscribe(entryFlow, subscriptionScope)
            val emissions = mutableListOf<List<DuplicateEntryCandidate>>()
            val job = subscriptionScope.launch(start = CoroutineStart.UNDISPATCHED) {
                results.take(2).toList(emissions)
            }

            testScheduler.advanceUntilIdle()
            entryFlow.value = initializedEntry
            testScheduler.advanceUntilIdle()
            job.join()

            emissions shouldBe listOf(emptyList(), enhancedCandidates)
            coVerify(exactly = 1) { enhanceDuplicateLibraryEntries(application, initializedEntry, baseCandidates) }
        } finally {
            subscriptionScope.cancel()
        }
    }

    private fun entry(id: Long, title: String, initialized: Boolean = true): Entry {
        return Entry.create().copy(
            id = id,
            source = id,
            title = title,
            initialized = initialized,
        )
    }

    private fun candidate(entryId: Long, score: Int, coverHashChecked: Boolean): DuplicateEntryCandidate {
        return DuplicateEntryCandidate(
            entry = entry(entryId, "Candidate $entryId"),
            count = 12,
            cheapScore = 40,
            scoreMax = 86,
            score = score,
            reasons = emptyList(),
            contentSignature = if (coverHashChecked) 1L else 0L,
        )
    }

    private class MutablePreference<T>(
        private val initialDefault: T,
    ) : Preference<T> {
        private val state = MutableStateFlow(initialDefault)

        override fun key(): String = "test"

        override fun get(): T = state.value

        override fun set(value: T) {
            state.value = value
        }

        override fun isSet(): Boolean = true

        override fun delete() {
            state.value = initialDefault
        }

        override fun defaultValue(): T = initialDefault

        override fun changes(): Flow<T> = state.asStateFlow()

        override fun stateIn(scope: CoroutineScope): StateFlow<T> {
            return changes().stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, get())
        }
    }
}
