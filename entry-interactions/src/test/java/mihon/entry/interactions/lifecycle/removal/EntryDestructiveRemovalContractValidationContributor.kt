package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureId
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry

class EntryDestructiveRemovalContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryDestructiveRemovalFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(
                    FeatureId("entry.destructive-removal"),
                    EntryDestructiveRemovalBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val entry = removalEntry(type)
                    var committed = false
                    val host = object : EntryDestructiveRemovalHost {
                        override suspend fun remove(
                            requested: List<Entry>,
                            beforeDelete: suspend (persisted: List<Entry>) -> Unit,
                        ): EntryDestructiveRemovalCommit {
                            beforeDelete(requested)
                            committed = true
                            return EntryDestructiveRemovalCommit.Applied(requested)
                        }
                    }
                    val composition = lifecycleContractComposition(
                        type,
                        EntryDestructiveRemovalFeatureContributor,
                        listOf(
                            ENTRY_DESTRUCTIVE_REMOVING_EXECUTION_POINT,
                            ENTRY_DESTRUCTIVE_REMOVED_EXECUTION_POINT,
                        ),
                    )
                    val result = EntryDestructiveRemovalCoordinator(host, composition.featureExecutions)
                        .remove(listOf(entry))
                    contractExpectation(
                        committed && result is EntryDestructiveRemovalResult.Removed,
                        "Destructive removal must commit through its host",
                    )
                }
            },
        )
    }
}

private fun removalEntry(type: EntryType) = Entry.create().copy(
    id = 82L,
    profileId = 1L,
    source = 9L,
    url = "/entry",
    title = "Entry",
    favorite = false,
    type = type,
)
