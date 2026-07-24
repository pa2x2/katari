package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryProgressLocator

class EntryProgressFeatureTest {
    private val source = Entry.create().copy(id = 7L, type = EntryType.BOOK)
    private val target = Entry.create().copy(id = 8L, type = EntryType.BOOK)
    private val snapshot = EntryProgressSnapshot(
        states = listOf(
            EntryProgressStateSnapshot(
                resourceKey = "chapter-1",
                locator = EntryProgressLocator(kind = "location", position = 12L),
            ),
        ),
    )
    private val mappings = listOf(
        EntryProgressResourceMapping(
            sourceResourceKey = "chapter-1",
            targetResourceKey = "chapter-2",
        ),
    )

    @Test
    fun `provider absence is valid and distinct from an available empty snapshot`() = runTest {
        val absent = compositionFor()
        val absentFeature = featureFor(absent)

        absentFeature.isApplicable(source.type) shouldBe false
        absentFeature.snapshot(source) shouldBe EntryProgressSnapshotResult.Inapplicable(source.type)
        absentFeature.restore(source, snapshot) shouldBe EntryProgressRestoreResult.Inapplicable(source.type)
        absentFeature.copy(source, target, mappings) shouldBe
            EntryProgressCopyResult.Inapplicable(setOf(source.type))

        val available = compositionFor(EntryProgressCapability.bind(RecordingProgressProcessor()))
        featureFor(available).snapshot(source) shouldBe
            EntryProgressSnapshotResult.Available(EntryProgressSnapshot())
    }

    @Test
    fun `progress provider owns backup operations without implying Migration`() = runTest {
        val processor = RecordingProgressProcessor(snapshot)
        val composition = compositionFor(EntryProgressCapability.bind(processor))
        val feature = featureFor(composition)

        feature.isApplicable(source.type) shouldBe true
        feature.snapshot(source) shouldBe EntryProgressSnapshotResult.Available(snapshot)
        feature.restore(source, snapshot) shouldBe EntryProgressRestoreResult.Applied
        feature.copy(source, target, mappings) shouldBe EntryProgressCopyResult.Inapplicable(setOf(source.type))

        processor.snapshottedEntries shouldBe listOf(source)
        processor.restored shouldBe listOf(source to snapshot)
        processor.copied shouldBe emptyList()
    }

    @Test
    fun `copy rejects mismatched entry types before provider dispatch`() = runTest {
        val processor = RecordingProgressProcessor(snapshot)
        val feature = featureFor(
            compositionFor(
                EntryProgressCapability.bind(processor),
                EntryMigrationCapability.bind(MigrationProvider()),
            ),
        )
        val animeTarget = target.copy(type = EntryType.ANIME)

        feature.copy(source, animeTarget, mappings) shouldBe
            EntryProgressCopyResult.IncompatibleTypes(EntryType.BOOK, EntryType.ANIME)
        processor.copied shouldBe emptyList()
    }

    @Test
    fun `migration preparation captures target-ready progress without invoking copy`() = runTest {
        val processor = RecordingProgressProcessor(snapshot)
        val feature = featureFor(
            compositionFor(
                EntryProgressCapability.bind(processor),
                EntryMigrationCapability.bind(MigrationProvider()),
            ),
        )

        val prepared = feature.prepareMigration(source, target, mappings)
            as EntryProgressMigrationPreparation.Prepared

        prepared.payload shouldBe EntryProgressMigrationPayload(
            target = target,
            snapshot = EntryProgressSnapshot(
                listOf(
                    snapshot.states.single().copy(
                        resourceKey = "chapter-2",
                        sourceChildKey = "chapter-2",
                    ),
                ),
            ),
        )
        processor.copied shouldBe emptyList()
        feature.applyMigration(prepared.payload) shouldBe EntryProgressRestoreResult.Applied
        processor.restored shouldBe listOf(target to prepared.payload.snapshot)
    }

    private fun compositionFor(
        vararg bindings: EntryInteractionProviderBinding<*>,
    ): EntryInteractionComposition {
        return createEntryInteractionComposition(
            plugins = listOf(
                object : EntryInteractionPlugin {
                    override val type = EntryType.BOOK
                    override val owner = ContributionOwner("test.partial-progress-type")
                    override val providerBindings = bindings.toList()
                },
            ),
            featureContributors = listOf(EntryProgressFeatureContributor),
        )
    }

    private fun featureFor(composition: EntryInteractionComposition): EntryProgressFeature {
        return DefaultEntryProgressFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.progress,
            repository = mockk(relaxed = true),
            getEntryWithChapters = mockk(relaxed = true),
            globalLibraryPreferences = mockk(relaxed = true),
        )
    }

    private class RecordingProgressProcessor(
        private val snapshot: EntryProgressSnapshot = EntryProgressSnapshot(),
    ) : EntryProgressProcessor {
        override val type = EntryType.BOOK
        val snapshottedEntries = mutableListOf<Entry>()
        val restored = mutableListOf<Pair<Entry, EntryProgressSnapshot>>()
        val copied = mutableListOf<Triple<Entry, Entry, List<EntryProgressResourceMapping>>>()

        override suspend fun snapshot(entry: Entry): EntryProgressSnapshot {
            snapshottedEntries += entry
            return snapshot
        }

        override suspend fun restore(entry: Entry, snapshot: EntryProgressSnapshot) {
            restored += entry to snapshot
        }

        override suspend fun copy(
            sourceEntry: Entry,
            targetEntry: Entry,
            resourceMappings: List<EntryProgressResourceMapping>,
        ) {
            copied += Triple(sourceEntry, targetEntry, resourceMappings)
        }
    }

    private class MigrationProvider : EntryMigrationProvider {
        override val type = EntryType.BOOK
    }
}
