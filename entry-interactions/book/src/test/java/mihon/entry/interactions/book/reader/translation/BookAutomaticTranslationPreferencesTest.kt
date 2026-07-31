package mihon.entry.interactions.book

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry

class BookAutomaticTranslationPreferencesTest {

    @Test
    fun `legacy global value seeds each reader once and readers then remain independent`() {
        val store = InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreferenceStore.InMemoryPreference(
                    BookAutomaticTranslationPreferences.LEGACY_GLOBAL_KEY,
                    true,
                    false,
                ),
            ),
        )
        val preferences = BookAutomaticTranslationPreferences(store)
        val prose = preferences.automaticSelectionEnabled(PROSE_SURFACE)
        val alternate = preferences.automaticSelectionEnabled(ALTERNATE_SURFACE)

        prose.get() shouldBe true
        alternate.get() shouldBe true

        prose.set(false)

        prose.get() shouldBe false
        alternate.get() shouldBe true

        prose.delete()

        prose.get() shouldBe false
        alternate.get() shouldBe true
    }

    @Test
    fun `profile owner claims legacy migration state and dynamic reader preferences`() {
        val registry = ProfilePreferenceOwnerRegistry()
        ProfilePreferenceOwnerInstaller(registry, ::InMemoryPreferenceStore).register(
            id = ProfilePreferenceOwnerId("entry-interactions.book.automatic-translation"),
            keyPatterns = setOf(
                ProfilePreferenceKeyPattern.Prefix(BookAutomaticTranslationPreferences.SURFACE_KEY_PREFIX),
            ),
            factory = ::BookAutomaticTranslationPreferences,
        )

        val ownership = registry.ownership(
            existingKeys = setOf(
                "${BookAutomaticTranslationPreferences.SURFACE_KEY_PREFIX}$PROSE_SURFACE",
                "${BookAutomaticTranslationPreferences.SURFACE_KEY_PREFIX}$ALTERNATE_SURFACE",
            ),
        )

        ownership.profileKeys.sorted() shouldContainExactly listOf(
            BookAutomaticTranslationPreferences.LEGACY_GLOBAL_KEY,
            BookAutomaticTranslationPreferences.MIGRATED_SURFACES_KEY,
            "${BookAutomaticTranslationPreferences.SURFACE_KEY_PREFIX}$ALTERNATE_SURFACE",
            "${BookAutomaticTranslationPreferences.SURFACE_KEY_PREFIX}$PROSE_SURFACE",
        ).sorted()
        ownership.appStateKeys shouldBe emptySet()
    }

    private companion object {
        const val PROSE_SURFACE = "builtin.book.prose.html"
        const val ALTERNATE_SURFACE = "test.book.alternate"
    }
}
