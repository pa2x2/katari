package eu.kanade.tachiyomi.ui.library

import eu.kanade.presentation.library.components.LibraryDisplaySettings
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.service.LibraryPreferences

class LibraryItemPreferenceFlowsTest {

    @Test
    fun `badge changes emit display settings without invalidating filters`() = runTest {
        val preferences = LibraryPreferences(InMemoryPreferenceStore())
        val filters = mutableListOf<LibraryFilterPreferences>()
        val displays = mutableListOf<LibraryDisplaySettings>()
        val filterCollection = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            observeLibraryFilterPreferences(preferences).toList(filters)
        }
        val displayCollection = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            observeLibraryDisplaySettings(preferences).toList(displays)
        }
        runCurrent()

        preferences.downloadBadge.set(true)
        runCurrent()

        filters.size shouldBe 1
        displays.map(LibraryDisplaySettings::downloadBadge) shouldContainExactly listOf(false, true)
        filterCollection.cancelAndJoin()
        displayCollection.cancelAndJoin()
    }

    @Test
    fun `filter changes emit policies without invalidating display settings`() = runTest {
        val preferences = LibraryPreferences(InMemoryPreferenceStore())
        val filters = mutableListOf<LibraryFilterPreferences>()
        val displays = mutableListOf<LibraryDisplaySettings>()
        val filterCollection = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            observeLibraryFilterPreferences(preferences).toList(filters)
        }
        val displayCollection = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            observeLibraryDisplaySettings(preferences).toList(displays)
        }
        runCurrent()

        preferences.filterDownloaded.set(TriState.ENABLED_IS)
        runCurrent()

        filters.map(LibraryFilterPreferences::filterDownloaded) shouldContainExactly listOf(
            TriState.DISABLED,
            TriState.ENABLED_IS,
        )
        displays.size shouldBe 1
        filterCollection.cancelAndJoin()
        displayCollection.cancelAndJoin()
    }

    @Test
    fun `initial snapshots retain display defaults and outside period policy`() = runTest {
        val preferences = LibraryPreferences(InMemoryPreferenceStore()).also {
            it.autoUpdateEntryRestrictions.set(emptySet())
            it.downloadedOnly.set(true)
            it.filterDownloaded.set(TriState.ENABLED_IS)
            it.filterUnread.set(TriState.ENABLED_NOT)
            it.filterNotStarted.set(TriState.ENABLED_IS)
            it.filterBookmarked.set(TriState.ENABLED_NOT)
            it.filterCompleted.set(TriState.ENABLED_IS)
            it.filterIntervalCustom.set(TriState.ENABLED_NOT)
        }

        observeLibraryFilterPreferences(preferences).first() shouldBe LibraryFilterPreferences(
            skipOutsideReleasePeriod = false,
            globalFilterDownloaded = true,
            filterDownloaded = TriState.ENABLED_IS,
            filterUnread = TriState.ENABLED_NOT,
            filterNotStarted = TriState.ENABLED_IS,
            filterBookmarked = TriState.ENABLED_NOT,
            filterCompleted = TriState.ENABLED_IS,
            filterIntervalCustom = TriState.ENABLED_NOT,
        )
        observeLibraryDisplaySettings(preferences).first() shouldBe LibraryDisplaySettings(
            downloadBadge = false,
            unreadBadge = true,
            localBadge = true,
            languageBadge = false,
            entryTypeBadge = true,
        )

        preferences.autoUpdateEntryRestrictions.set(setOf(LibraryPreferences.ENTRY_OUTSIDE_RELEASE_PERIOD))
        observeLibraryFilterPreferences(preferences).first().skipOutsideReleasePeriod shouldBe
            true
    }
}
