package eu.kanade.tachiyomi.ui.library

import io.kotest.matchers.shouldBe
import mihon.feature.profiles.core.Profile
import org.junit.jupiter.api.Test

class LibraryProfileMoveTest {

    @Test
    fun `available destinations exclude current and archived profiles`() {
        val profiles = listOf(
            profile(1L),
            profile(2L),
            profile(3L, archived = true),
        )

        availableMoveProfiles(profiles, sourceProfileId = 1L, requiresUnlock = { false })
            .map(Profile::id) shouldBe listOf(2L)
    }

    @Test
    fun `available destinations expose current unlock requirement`() {
        val destination = availableMoveProfiles(
            profiles = listOf(profile(1L), profile(2L)),
            sourceProfileId = 1L,
            requiresUnlock = { it == 2L },
        ).single()

        destination.requiresAuth shouldBe true
    }

    private fun profile(id: Long, archived: Boolean = false) = Profile(
        id = id,
        uuid = "profile-$id",
        name = "Profile $id",
        colorSeed = id,
        position = id,
        requiresAuth = false,
        isArchived = archived,
    )
}
