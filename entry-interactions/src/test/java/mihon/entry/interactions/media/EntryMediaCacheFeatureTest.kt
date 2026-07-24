package mihon.entry.interactions

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class EntryMediaCacheFeatureTest {
    @Test
    fun `provider absence is valid and produces no settings`() {
        val feature = feature(plugin())
        val missingId = EntryMediaCacheId("future.missing-cache")

        feature.settings() shouldBe emptyList()
        feature.clear(missingId) shouldBe EntryMediaCacheClearResult.Inapplicable(missingId)
        feature.clearEnabledOnLaunch() shouldBe emptyList()
    }

    @Test
    fun `one provider activates every shared cache behavior`() {
        val artifact = TestArtifact(
            id = EntryMediaCacheId("future.cache"),
            autoClearPreference = EntryMediaCacheAutoClearPreference("future_auto_clear"),
        )
        val feature = feature(plugin(provider(artifact)))

        val setting = feature.settings().single()
        setting.id shouldBe artifact.id
        setting.readableSize shouldBe "10 B"
        setting.autoClearOnLaunch.key() shouldBe "future_auto_clear"

        feature.clear(artifact.id).shouldBeInstanceOf<EntryMediaCacheClearResult.Cleared>().apply {
            deletedFiles shouldBe 2
            readableSize shouldBe "0 B"
        }
        feature.settings().single().readableSize shouldBe "0 B"

        setting.autoClearOnLaunch.set(true)
        feature.clearEnabledOnLaunch().single().shouldBeInstanceOf<EntryMediaCacheClearResult.Cleared>()
        artifact.clearCount shouldBe 2
    }

    @Test
    fun `launch clear reports each failure without suppressing unrelated caches`() {
        val failed = TestArtifact(
            id = EntryMediaCacheId("future.failed"),
            autoClearPreference = EntryMediaCacheAutoClearPreference("future_failed_auto_clear"),
            clearAction = { error("cannot clear") },
        )
        val cleared = TestArtifact(
            id = EntryMediaCacheId("future.cleared"),
            autoClearPreference = EntryMediaCacheAutoClearPreference("future_cleared_auto_clear"),
        )
        val feature = feature(plugin(provider(failed, cleared)))
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

        feature(plugin(provider(artifact)), store).settings().single().autoClearOnLaunch.get() shouldBe true

        val alreadyMigratedStore = InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreferenceStore.InMemoryPreference("legacy_auto_clear", true, false),
                InMemoryPreferenceStore.InMemoryPreference("future_auto_clear", false, false),
            ),
        )
        feature(plugin(provider(artifact)), alreadyMigratedStore)
            .settings().single().autoClearOnLaunch.get() shouldBe false
    }

    @Test
    fun `empty providers and duplicate stable ids fail composition explicitly`() {
        shouldThrow<IllegalStateException> {
            feature(plugin(provider()))
        }
        val duplicateId = EntryMediaCacheId("future.duplicate")
        shouldThrow<IllegalStateException> {
            feature(
                plugin(
                    provider(
                        TestArtifact(duplicateId, EntryMediaCacheAutoClearPreference("first")),
                        TestArtifact(duplicateId, EntryMediaCacheAutoClearPreference("second")),
                    ),
                ),
            )
        }
    }

    private fun feature(
        plugin: EntryInteractionPlugin,
        store: InMemoryPreferenceStore = InMemoryPreferenceStore(),
    ): EntryMediaCacheFeature {
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin),
            featureContributors = listOf(EntryMediaCacheFeatureContributor),
        )
        return DefaultEntryMediaCacheFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.mediaCache,
            preferenceStore = store,
        )
    }

    private fun plugin(provider: EntryMediaCacheProvider? = null): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = EntryType.BOOK
            override val owner = ContributionOwner("test.future-type")
            override val providerBindings = provider?.let { listOf(EntryMediaCacheCapability.bind(it)) }.orEmpty()
        }
    }

    private fun provider(vararg artifacts: EntryMediaCacheArtifact): EntryMediaCacheProvider {
        return object : EntryMediaCacheProvider {
            override val type = EntryType.BOOK
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
        override val readableSize: String get() = if (clearCount == 0) "10 B" else "0 B"
        var clearCount = 0
            private set

        override fun clear(): Int {
            clearCount++
            return clearAction()
        }
    }
}
