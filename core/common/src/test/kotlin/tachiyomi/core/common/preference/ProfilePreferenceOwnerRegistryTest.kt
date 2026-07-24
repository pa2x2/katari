package tachiyomi.core.common.preference

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

class ProfilePreferenceOwnerRegistryTest {

    @Test
    fun `owner ids are unique`() {
        val installer = installer(ProfilePreferenceOwnerRegistry())
        installer.register(ProfilePreferenceOwnerId("feature"), factory = ::FirstPreferences)

        shouldThrow<IllegalStateException> {
            installer.register(ProfilePreferenceOwnerId("feature"), factory = ::SecondPreferences)
        }
    }

    @Test
    fun `overlapping dynamic families are rejected`() {
        val installer = installer(ProfilePreferenceOwnerRegistry())
        installer.register(
            id = ProfilePreferenceOwnerId("feature"),
            keyPatterns = setOf(ProfilePreferenceKeyPattern.Prefix("feature_item_")),
            factory = ::FirstPreferences,
        )

        shouldThrow<IllegalStateException> {
            installer.register(
                id = ProfilePreferenceOwnerId("other"),
                keyPatterns = setOf(ProfilePreferenceKeyPattern.Prefix("feature_item_special_")),
                factory = ::SecondPreferences,
            )
        }
    }

    @Test
    fun `static keys cannot have multiple owners`() {
        val registry = ProfilePreferenceOwnerRegistry()
        val installer = installer(registry)
        installer.register(ProfilePreferenceOwnerId("first"), factory = ::FirstPreferences)
        installer.register(ProfilePreferenceOwnerId("second"), factory = ::SecondPreferences)

        shouldThrow<IllegalStateException> { registry.ownership() }
    }

    @Test
    fun `static keys cannot be captured by another owners dynamic family`() {
        val registry = ProfilePreferenceOwnerRegistry()
        val installer = installer(registry)
        installer.register(ProfilePreferenceOwnerId("static"), factory = ::FirstPreferences)
        installer.register(
            id = ProfilePreferenceOwnerId("dynamic"),
            keyPatterns = setOf(ProfilePreferenceKeyPattern.Prefix("shared_")),
            factory = ::PatternPreferences,
        )

        shouldThrow<IllegalStateException> { registry.ownership() }
    }

    @Test
    fun `ownership evaluation seals installation`() {
        val registry = ProfilePreferenceOwnerRegistry()
        val installer = installer(registry)
        installer.register(ProfilePreferenceOwnerId("first"), factory = ::FirstPreferences)

        registry.ownership()

        shouldThrow<IllegalStateException> {
            installer.register(ProfilePreferenceOwnerId("late"), factory = ::LatePreferences)
        }
    }

    private fun installer(registry: ProfilePreferenceOwnerRegistry): ProfilePreferenceOwnerInstaller {
        return ProfilePreferenceOwnerInstaller(registry) { InMemoryPreferenceStore() }
    }
}

private class FirstPreferences(store: PreferenceStore) {
    val value = store.getBoolean("shared_key")
}

private class SecondPreferences(store: PreferenceStore) {
    val value = store.getBoolean("shared_key")
}

private class LatePreferences(store: PreferenceStore) {
    val value = store.getBoolean("late_key")
}

private class PatternPreferences(store: PreferenceStore) {
    val value = store.getBoolean("different_key")
}
