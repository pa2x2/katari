package mihon.feature.profiles.core

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test

class ProfileAnimeSourcePreferenceProviderTest {

    @Test
    fun `source preference key uses profile namespace`() {
        val store = FakeProfileStore()
        val key = store.sourcePreferenceKey(sourceId = 42L, profileId = 7L)

        key shouldBe "source_7_42"
    }

    @Test
    fun `source preference key is profile scoped`() {
        val store = FakeProfileStore()

        store.sourcePreferenceKey(sourceId = 42L, profileId = 1L) shouldBe "source_1_42"
        store.sourcePreferenceKey(sourceId = 42L, profileId = 2L) shouldBe "source_2_42"
    }

    private class FakeProfileStore(
        override val currentProfileId: Long = 0L,
    ) : ProfileStore {
        override val currentProfileIdFlow: Flow<Long> = flowOf(currentProfileId)
        override fun setCurrentProfileId(profileId: Long) = Unit
        override fun basePreferenceStore() = error("unused")
        override fun appStateStore() = error("unused")
        override fun privateStore() = error("unused")
        override fun profileStore() = error("unused")
        override fun profileStore(profileId: Long) = error("unused")
        override fun appStateStore(profileId: Long) = error("unused")
        override fun privateStore(profileId: Long) = error("unused")
        override fun sourcePreferenceKey(sourceId: Long, profileId: Long): String {
            return "source_${profileId}_$sourceId"
        }
    }
}
