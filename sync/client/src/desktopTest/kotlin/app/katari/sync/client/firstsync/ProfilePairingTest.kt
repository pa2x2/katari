package app.katari.sync.client.firstsync

import app.katari.sync.client.kind.profile.ProfileKind
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ProfilePairingTest {

    @Test
    fun `profiles shared before the server started over pair with themselves, not with another of the same name`() {
        // Three "Second" profiles shared before the loss, listed by the refilled account in another order, one never
        // shared, and an unrelated "Second" another device added to the account
        val local = listOf(
            LocalProfile(1, ProfileKind.DEFAULT_UUID, "Default"),
            LocalProfile(2, "x1", "Second"),
            LocalProfile(3, "x2", "Second"),
            LocalProfile(4, "x3", "Second"),
            LocalProfile(5, "mine", "Second"),
        )
        val account = listOf(
            AccountProfile("x2", "Second"),
            AccountProfile("other", "second "),
            AccountProfile("x3", "Second"),
            AccountProfile(ProfileKind.DEFAULT_UUID, "Default"),
            AccountProfile("x1", "Second"),
        )

        proposePairings(local, account).map { Triple(it.profileId, it.pairedWith?.uuid, it.sameProfile) } shouldBe
            listOf(
                Triple(1L, ProfileKind.DEFAULT_UUID, true),
                Triple(2L, "x1", true),
                Triple(3L, "x2", true),
                Triple(4L, "x3", true),
                Triple(5L, "other", false),
            )
    }
}
