package mihon.feature.profiles.core

import android.content.Context
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import mihon.domain.profile.ProfileSeedPreferences
import mihon.feature.profiles.FakeSharedPreferences
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.domain.library.service.DuplicatePreferences

class ProfileSeedKeysTest {

    @Test
    fun `what the app seeded reads as untouched under the key sync lists, not the key Android files it under`() {
        val profileId = MutableStateFlow(1L)
        val store = ProfileAwarePreferenceStore(
            backing = AndroidPreferenceStore(
                context = mockk<Context>(relaxed = true),
                sharedPreferences = FakeSharedPreferences(),
            ),
            profileProvider = { profileId.value },
            profileFlow = profileId,
            namespace = ProfileAwarePreferenceStore.Namespace.PROFILE,
        )

        ProfileSeedPreferences.seed(store)

        ProfileSeedPreferences.untouched(store) shouldBe setOf("extended_duplicate_detection_title_exclusion_patterns")
        DuplicatePreferences(store).titleExclusionPatterns.set(listOf("[*]"))
        ProfileSeedPreferences.untouched(store) shouldBe emptySet()
    }
}
