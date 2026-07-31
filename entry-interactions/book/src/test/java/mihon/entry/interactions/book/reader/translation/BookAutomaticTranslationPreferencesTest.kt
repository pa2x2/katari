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
        val document = preferences.automaticSelectionEnabled(DOCUMENT_SURFACE)
        val alternate = preferences.automaticSelectionEnabled(ALTERNATE_SURFACE)

        document.get() shouldBe true
        alternate.get() shouldBe true

        document.set(false)

        document.get() shouldBe false
        alternate.get() shouldBe true

        document.delete()

        document.get() shouldBe false
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
                "${BookAutomaticTranslationPreferences.SURFACE_KEY_PREFIX}$DOCUMENT_SURFACE",
                "${BookAutomaticTranslationPreferences.SURFACE_KEY_PREFIX}$ALTERNATE_SURFACE",
            ),
        )

        ownership.profileKeys.sorted() shouldContainExactly listOf(
            BookAutomaticTranslationPreferences.LEGACY_GLOBAL_KEY,
            BookAutomaticTranslationPreferences.MIGRATED_SURFACES_KEY,
            "${BookAutomaticTranslationPreferences.SURFACE_KEY_PREFIX}$ALTERNATE_SURFACE",
            "${BookAutomaticTranslationPreferences.SURFACE_KEY_PREFIX}$DOCUMENT_SURFACE",
        ).sorted()
        ownership.appStateKeys shouldBe emptySet()
    }

    private companion object {
        const val DOCUMENT_SURFACE = "builtin.book.document"
        const val ALTERNATE_SURFACE = "test.book.alternate"
    }
}
