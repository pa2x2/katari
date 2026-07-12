package mihon.feature.profiles.core

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ProfileNameValidationTest {

    @Test
    fun `name conflict matches any profile`() {
        val profiles = listOf(
            profile(id = 1L, name = "Default"),
            profile(id = 2L, name = "Other"),
        )

        profiles.hasNameConflict(name = "default") shouldBe true
    }

    @Test
    fun `name conflict ignores the renamed profile`() {
        val profiles = listOf(
            profile(id = 1L, name = "Default"),
            profile(id = 2L, name = "Anime"),
        )

        profiles.hasNameConflict(
            name = " default ",
            excludedProfileId = 1L,
        ) shouldBe false
    }

    private fun profile(id: Long, name: String) = Profile(
        id = id,
        uuid = "uuid-$id",
        name = name,
        colorSeed = id,
        position = id,
        requiresAuth = false,
        isArchived = false,
    )
}
