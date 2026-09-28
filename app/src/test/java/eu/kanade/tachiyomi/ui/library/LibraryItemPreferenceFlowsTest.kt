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
    fun `badge and filter changes only invalidate their own flow`() = runTest {
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
        preferences.filterDownloaded.set(TriState.ENABLED_IS)
        runCurrent()

        displays.map(LibraryDisplaySettings::downloadBadge) shouldContainExactly listOf(false, true)
        filters.map(LibraryFilterPreferences::filterDownloaded) shouldContainExactly listOf(
            TriState.DISABLED,
            TriState.ENABLED_IS,
        )
        filterCollection.cancelAndJoin()
        displayCollection.cancelAndJoin()
    }
}
