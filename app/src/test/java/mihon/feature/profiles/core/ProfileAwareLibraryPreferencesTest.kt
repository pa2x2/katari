package mihon.feature.profiles.core

import android.content.Context
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import mihon.core.common.CustomPreferences
import mihon.core.common.HomeScreenTabs
import mihon.core.common.defaultHomeScreenTabs
import mihon.feature.profiles.FakeSharedPreferences
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.DuplicateTitleExclusions
import tachiyomi.domain.library.service.LibraryPreferences

class ProfileAwareLibraryPreferencesTest {

    @Test
    fun `settings of every storage kind stay isolated per profile`() {
        val fixture = createFixture()
        val sort = LibrarySort(LibrarySort.Type.DateAdded, LibrarySort.Direction.Descending)
        val tabs = setOf(HomeScreenTabs.Library.name, HomeScreenTabs.More.name)

        fixture.libraryPreferences.defaultCategory.set(3)
        fixture.libraryPreferences.downloadedOnly.set(true)
        fixture.libraryPreferences.sortingMode.set(sort)
        fixture.customPreferences.homeScreenTabs.set(tabs)
        fixture.duplicatePreferences.titleExclusionPatterns.set(listOf("[*]"))

        fixture.activeProfileId.value = 2L

        fixture.libraryPreferences.defaultCategory.get() shouldBe -1
        fixture.libraryPreferences.downloadedOnly.get() shouldBe false
        fixture.libraryPreferences.sortingMode.get() shouldBe LibrarySort.default
        fixture.customPreferences.homeScreenTabs.get() shouldBe defaultHomeScreenTabs()
        fixture.duplicatePreferences.titleExclusionPatterns.get() shouldBe DuplicateTitleExclusions.defaultPatterns
        fixture.libraryPreferences.defaultCategory.set(7)

        fixture.activeProfileId.value = 1L

        fixture.libraryPreferences.defaultCategory.get() shouldBe 3
        fixture.libraryPreferences.downloadedOnly.get() shouldBe true
        fixture.libraryPreferences.sortingMode.get() shouldBe sort
        fixture.customPreferences.homeScreenTabs.get() shouldBe tabs
        fixture.duplicatePreferences.titleExclusionPatterns.get() shouldBe listOf("[*]")
    }

    private fun createFixture(): Fixture {
        val activeProfileId = MutableStateFlow(1L)
        val backing = AndroidPreferenceStore(
            context = mockk<Context>(relaxed = true),
            sharedPreferences = FakeSharedPreferences(),
        )
        val preferenceStore = ProfileAwarePreferenceStore(
            backing = backing,
            profileProvider = { activeProfileId.value },
            profileFlow = activeProfileId,
            namespace = ProfileAwarePreferenceStore.Namespace.PROFILE,
        )
        return Fixture(
            activeProfileId = activeProfileId,
            libraryPreferences = LibraryPreferences(preferenceStore),
            duplicatePreferences = DuplicatePreferences(preferenceStore),
            customPreferences = CustomPreferences(preferenceStore),
        )
    }

    private data class Fixture(
        val activeProfileId: MutableStateFlow<Long>,
        val libraryPreferences: LibraryPreferences,
        val duplicatePreferences: DuplicatePreferences,
        val customPreferences: CustomPreferences,
    )
}
