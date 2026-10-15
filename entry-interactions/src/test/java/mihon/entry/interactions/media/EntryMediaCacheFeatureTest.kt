package mihon.entry.interactions.media

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import mihon.entry.interactions.runtime.DefaultEntryInteractions
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class EntryMediaCacheFeatureTest {
    @Test
    fun `launch clear reports each failure without suppressing unrelated caches`() {
        val failed = TestArtifact(
            id = EntryMediaCacheId("future.failed"),
            autoClearPreference = EntryMediaCacheAutoClearPreference(
                "future_failed_auto_clear",
            ),
            clearAction = { error("cannot clear") },
        )
        val cleared = TestArtifact(
            id = EntryMediaCacheId("future.cleared"),
            autoClearPreference = EntryMediaCacheAutoClearPreference(
                "future_cleared_auto_clear",
            ),
        )
        val feature = feature(provider(failed, cleared))
        feature.settings().forEach { it.autoClearOnLaunch.set(true) }

        feature.clearEnabledOnLaunch().map { it::class } shouldContainExactly listOf(
            EntryMediaCacheClearResult.Failed::class,
            EntryMediaCacheClearResult.Cleared::class,
        )
        cleared.clearCount shouldBe 1
    }

    @Test
    fun `legacy preference seed is a one-time compatibility rule`() {
        val store = InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreferenceStore.InMemoryPreference("legacy_auto_clear", true, false),
            ),
        )
        val artifact = TestArtifact(
            id = EntryMediaCacheId("future.seeded"),
            autoClearPreference = EntryMediaCacheAutoClearPreference(
                key = "future_auto_clear",
                seedFromKeyWhenAbsent = "legacy_auto_clear",
            ),
        )

        feature(provider(artifact), store).settings().single().autoClearOnLaunch.get() shouldBe true

        val alreadyMigratedStore = InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreferenceStore.InMemoryPreference("legacy_auto_clear", true, false),
                InMemoryPreferenceStore.InMemoryPreference("future_auto_clear", false, false),
            ),
        )
        feature(provider(artifact), alreadyMigratedStore)
            .settings().single().autoClearOnLaunch.get() shouldBe false
    }

    private fun feature(
        provider: EntryMediaCacheProvider,
        store: InMemoryPreferenceStore = InMemoryPreferenceStore(),
    ): EntryMediaCacheFeature {
        return DefaultEntryMediaCacheFeature(
            applicableTypes = setOf(EntryType.BOOK),
            interaction = DefaultEntryInteractions(mediaCacheProviders = mapOf(EntryType.BOOK to provider)).mediaCache,
            preferenceStore = store,
        )
    }

    private fun provider(vararg artifacts: EntryMediaCacheArtifact): EntryMediaCacheProvider {
        return object : EntryMediaCacheProvider {
            override val artifacts = artifacts.toList()
        }
    }

    private class TestArtifact(
        override val id: EntryMediaCacheId,
        override val autoClearPreference: EntryMediaCacheAutoClearPreference,
        private val clearAction: () -> Int = { 2 },
    ) : EntryMediaCacheArtifact {
        override val clearLabel: StringResource = mockk()
        override val autoClearLabel: StringResource = mockk()
        override val readableSize: String = "0 B"
        var clearCount = 0
            private set

        override fun clear(): Int {
            clearCount++
            return clearAction()
        }
    }
}
