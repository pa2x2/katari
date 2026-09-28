package mihon.entry.interactions.media

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.SourceMetadata
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import mihon.entry.interactions.child.DefaultEntryChildListFeature
import mihon.entry.interactions.child.EntryChildListFeatureContributor
import mihon.entry.interactions.runtime.EntryImmersiveCapability
import mihon.entry.interactions.runtime.EntryImmersiveLoadMode
import mihon.entry.interactions.runtime.EntryImmersiveProcessor
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryImmersiveFeatureTest {
    private val entry = Entry.create().copy(id = 1L, type = EntryType.BOOK)

    @Test
    fun `declared source types only prune the source surface and never reject returned entry type`() {
        val feature = featureFor(EntryImmersiveCapability.bind(BookImmersiveProcessor))
        val declaredOtherType = mockk<EntryCatalogueSource>(
            relaxed = true,
            moreInterfaces = arrayOf(SourceMetadata::class),
        )
        every { declaredOtherType.supportsImmersiveFeed } returns true
        every { (declaredOtherType as SourceMetadata).supportedEntryTypes } returns setOf(EntryType.MANGA)

        feature.sourceAvailability(declaredOtherType) shouldBe
            EntryImmersiveSourceAvailability.NoCompatibleDeclaredType(setOf(EntryType.MANGA))
        feature.availability(EntryImmersiveContext(entry, declaredOtherType))
            .shouldBeInstanceOf<EntryImmersiveAvailability.Available>()
    }

    private fun featureFor(vararg bindings: EntryInteractionProviderBinding<*>): EntryImmersiveFeature {
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin(*bindings)),
            featureContributors = listOf(
                EntryImmersiveFeatureContributor,
                EntryChildListFeatureContributor,
            ),
        )
        val childList = DefaultEntryChildListFeature(
            evaluation = composition.featureGraphEvaluation,
            childList = composition.interactions.childList,
            childProgress = composition.interactions.childProgress,
            missingChildGap = composition.interactions.missingChildGap,
        )
        return DefaultEntryImmersiveFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.immersive,
            childList = childList,
            sourceRefresh = mockk(),
        )
    }

    private fun plugin(vararg bindings: EntryInteractionProviderBinding<*>): EntryInteractionPlugin =
        object : EntryInteractionPlugin {
            override val type = EntryType.BOOK
            override val owner = ContributionOwner("test.type.anonymous")
            override val providerBindings = bindings.toList()
        }

    private object BookImmersiveProcessor : EntryImmersiveProcessor {
        override val type = EntryType.BOOK
        override val loadMode = EntryImmersiveLoadMode.ENTRY
        override val preloadRadius = 2

        override suspend fun load(
            context: Context,
            entry: Entry,
            chapter: EntryChapter?,
            source: UnifiedSource,
        ): EntryImmersiveHandle = error("Not loaded by availability checks")

        override fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer = error("Not rendered")

        override suspend fun persistProgress(handle: EntryImmersiveHandle, progress: EntryImmersiveProgress) = Unit

        override fun release(handle: EntryImmersiveHandle) = Unit
    }
}
