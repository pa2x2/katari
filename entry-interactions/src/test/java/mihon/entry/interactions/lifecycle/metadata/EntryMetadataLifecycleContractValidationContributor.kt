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

class EntryMetadataLifecycleContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryMetadataLifecycleFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(
                    FeatureId("entry.metadata-lifecycle"),
                    EntryMetadataLifecycleBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val entry = metadataEntry(type)
                    val composition = lifecycleContractComposition(
                        type,
                        EntryMetadataLifecycleFeatureContributor,
                        listOf(ENTRY_METADATA_CHANGED_EXECUTION_POINT),
                    )
                    val feature = EntryMetadataLifecycleCoordinator(composition.featureExecutions)
                    contractExpectation(
                        feature.changed(entry, entry.copy(title = "Changed")) is EntryMetadataChangeResult.Applied,
                        "Metadata lifecycle must accept a persisted change",
                    )
                }
            },
        )
    }
}

private fun metadataEntry(type: EntryType) = Entry.create().copy(
    id = 81L,
    profileId = 1L,
    source = 9L,
    url = "/entry",
    title = "Entry",
    favorite = true,
    type = type,
)
