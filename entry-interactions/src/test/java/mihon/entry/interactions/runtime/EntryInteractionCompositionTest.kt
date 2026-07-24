package mihon.entry.interactions

import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.CapabilityDefinition
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.featureGraphContributor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryInteractionCompositionTest {
    private val context = mockk<Context>(relaxed = true)
    private val chapter = EntryChapter.create().copy(id = 10L)

    @Test
    fun `empty composition is valid and optional interactions are absent`() {
        val interactions = createEntryInteractions(emptyList(), emptyList())
        val entry = entry(EntryType.BOOK)

        interactions.download.hasDownloads(entry).shouldBeFalse()
        interactions.preview.processor(entry.type).shouldBeNull()
    }

    @Test
    fun `provider bindings dispatch by their contributed type`() {
        val opened = mutableListOf<EntryType>()
        val manga = RecordingOpenProcessor(EntryType.MANGA, opened)
        val anime = RecordingOpenProcessor(EntryType.ANIME, opened)
        val interactions = createEntryInteractions(
            plugins = listOf(
                plugin(EntryType.MANGA, EntryOpenCapability.bind(manga)),
                plugin(EntryType.ANIME, EntryOpenCapability.bind(anime)),
            ),
            featureContributors = listOf(featureUsing(EntryOpenCapability.definition)),
        )

        interactions.open.open(context, entry(EntryType.ANIME), chapter)
        interactions.open.open(context, entry(EntryType.MANGA), chapter)

        opened.shouldContainExactly(EntryType.ANIME, EntryType.MANGA)
    }

    @Test
    fun `a contribution containing only one provider remains valid`() {
        val processor = RecordingOpenProcessor(EntryType.BOOK, mutableListOf())
        val interactions = createEntryInteractions(
            plugins = listOf(plugin(EntryType.BOOK, EntryOpenCapability.bind(processor))),
            featureContributors = listOf(featureUsing(EntryOpenCapability.definition)),
        )
        val book = entry(EntryType.BOOK)

        interactions.open.open(context, book, chapter)
        interactions.download.hasDownloads(book).shouldBeFalse()
    }

    @Test
    fun `duplicate provider binding fails generically`() {
        val first = RecordingOpenProcessor(EntryType.MANGA, mutableListOf())
        val second = RecordingOpenProcessor(EntryType.MANGA, mutableListOf())

        val exception = assertThrows<IllegalArgumentException> {
            plugin(
                EntryType.MANGA,
                EntryOpenCapability.bind(first),
                EntryOpenCapability.bind(second),
            )
        }

        exception.message.orEmpty() shouldContain "Capability providers"
    }

    @Test
    fun `plugin rejects a provider owned by another type`() {
        val exception = assertThrows<IllegalArgumentException> {
            val plugin = plugin(
                EntryType.MANGA,
                EntryOpenCapability.bind(RecordingOpenProcessor(EntryType.ANIME, mutableListOf())),
            )
            createEntryInteractions(
                plugins = listOf(plugin),
                featureContributors = listOf(featureUsing(EntryOpenCapability.definition)),
            )
        }

        exception.message.orEmpty() shouldContain "cannot contribute"
    }

    @Test
    fun `plugin rejects a specialized adapter owned by another type`() {
        val plugin = object : EntryInteractionPlugin {
            override val type = EntryType.MANGA
            override val owner = ContributionOwner("test.type.manga")
            override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
            override val specializedAdapters = listOf(
                EntryChildWebViewHostContribution.bind(
                    object : EntryChildWebViewHostAdapter {
                        override val type = EntryType.ANIME
                    },
                ),
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            createEntryInteractions(listOf(plugin), emptyList())
        }

        exception.message.orEmpty() shouldContain "cannot contribute specialized adapter"
    }

    @Test
    fun `continue facade executes the selected provider behavior`() = runTest {
        val opened = mutableListOf<Long>()
        val processor = object : EntryContinueProcessor {
            override val type = EntryType.ANIME

            override suspend fun findNext(entry: Entry): EntryChapter = chapter

            override fun open(context: Context, entry: Entry, chapter: EntryChapter) {
                opened += chapter.id
            }
        }
        val interactions = createEntryInteractions(
            plugins = listOf(plugin(EntryType.ANIME, EntryContinueCapability.bind(processor))),
            featureContributors = listOf(featureUsing(EntryContinueCapability.definition)),
        )

        interactions.continueEntry.continueEntry(context, entry(EntryType.ANIME))

        opened.shouldContainExactly(chapter.id)
    }

    private fun plugin(
        type: EntryType,
        vararg bindings: EntryInteractionProviderBinding<*>,
    ): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = type
            override val owner = ContributionOwner("test.type.${type.name.lowercase()}")
            override val providerBindings = bindings.toList()
        }.also(EntryInteractionPlugin::validateContribution)
    }

    private fun featureUsing(capability: CapabilityDefinition<*>): FeatureGraphContributor {
        val owner = ContributionOwner("test.feature.${capability.id.value}")
        val suffix = capability.id.value
        return featureGraphContributor(owner) {
            add(
                FeatureContribution(
                    feature = FeatureId("test.$suffix"),
                    owner = owner,
                    integrations = listOf(
                        FeatureIntegration(
                            id = FeatureIntegrationId("test.$suffix.integration"),
                            prerequisites = CapabilityExpression.Provided(capability),
                            behaviorProjections = listOf(
                                object : FeatureBehaviorProjection {
                                    override val id = FeatureArtifactId("test.$suffix.behavior")
                                },
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    private fun entry(type: EntryType): Entry = Entry.create().copy(type = type)

    private class RecordingOpenProcessor(
        override val type: EntryType,
        private val opened: MutableList<EntryType>,
    ) : EntryOpenProcessor {
        override fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
            opened += entry.type
        }

        override fun pendingIntent(
            context: Context,
            entry: Entry,
            chapter: EntryChapter,
            options: EntryOpenOptions,
        ): PendingIntent = mockk(relaxed = true)
    }
}
