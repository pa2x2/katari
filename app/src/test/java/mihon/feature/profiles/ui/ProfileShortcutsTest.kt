package mihon.feature.profiles.ui

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import mihon.feature.profiles.core.Profile
import org.junit.jupiter.api.Test

class ProfileShortcutsTest {

    @Test
    fun `returns other profile when exactly two profiles exist`() {
        val profiles = listOf(
            profile(id = 1L, name = "Default"),
            profile(id = 2L, name = "Work"),
        )

        resolveProfileShortcutTarget(profiles, activeProfileId = 1L)?.id shouldBe 2L
    }

    @Test
    fun `returns null when active profile is missing`() {
        val profiles = listOf(
            profile(id = 1L, name = "Default"),
            profile(id = 2L, name = "Work"),
        )

        resolveProfileShortcutTarget(profiles, activeProfileId = null).shouldBeNull()
    }

    @Test
    fun `returns null when profile count is not two`() {
        resolveProfileShortcutTarget(
            profiles = listOf(profile(id = 1L, name = "Default")),
            activeProfileId = 1L,
        ).shouldBeNull()

        resolveProfileShortcutTarget(
            profiles = listOf(
                profile(id = 1L, name = "Default"),
                profile(id = 2L, name = "Work"),
                profile(id = 3L, name = "Guest"),
            ),
            activeProfileId = 1L,
        ).shouldBeNull()
    }

    @Test
    fun `returns null when active profile is not in visible profiles`() {
        val profiles = listOf(
            profile(id = 1L, name = "Default"),
            profile(id = 2L, name = "Work"),
        )

        resolveProfileShortcutTarget(profiles, activeProfileId = 3L).shouldBeNull()
    }

    private fun profile(id: Long, name: String) = Profile(
        id = id,
        uuid = "uuid-$id",
        name = name,
        colorSeed = 0L,
        position = id,
        requiresAuth = false,
        isArchived = false,
    )
}
