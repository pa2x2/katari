package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureId
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry

class EntryProfileMoveContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryProfileMoveFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(FeatureId("entry.profile-move"), EntryProfileMoveBehaviorContract),
            ) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val entry = profileMoveEntry(type)
                    var beforeCore = false
                    var afterCore = false
                    val host = object : EntryProfileMoveHost {
                        override suspend fun selectedEntries(request: EntryProfileMoveRequest) = listOf(entry)

                        override suspend fun destinationConflicts(
                            request: EntryProfileMoveRequest,
                            sourceEntries: List<Entry>,
                        ) = emptyList<EntryProfileMoveConflict>()

                        override suspend fun execute(
                            preview: EntryProfileMovePreview,
                            plan: EntryProfileMovePlan,
                            beforeCoreMutation: suspend () -> Unit,
                            afterCoreMutation: suspend () -> Unit,
                        ): EntryProfileMoveCommit {
                            beforeCoreMutation()
                            beforeCore = true
                            afterCoreMutation()
                            afterCore = true
                            return EntryProfileMoveCommit.Applied
                        }
                    }
                    val composition = lifecycleContractComposition(
                        type,
                        EntryProfileMoveFeatureContributor,
                        listOf(
                            ENTRY_PROFILE_MOVE_PREPARING_EXECUTION_POINT,
                            ENTRY_PROFILE_MOVE_DESTINATION_INSPECTING_EXECUTION_POINT,
                            ENTRY_PROFILE_MOVING_EXECUTION_POINT,
                            ENTRY_PROFILE_STATE_MOVED_EXECUTION_POINT,
                            ENTRY_PROFILE_MOVED_EXECUTION_POINT,
                        ),
                    )
                    val feature = EntryProfileMoveCoordinator(host, composition.featureExecutions)
                    val preview = feature.preview(EntryProfileMoveRequest(1L, 2L, null, listOf(entry.id)))
                    val result = feature.execute(preview, emptyMap())
                    contractExpectation(
                        beforeCore && afterCore && result.movedSelectedItemCount == 1,
                        "Profile movement must bracket core persistence with transactional participation",
                    )
                }
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_PROFILE_MOVE_SOURCE_VISIBILITY_PARTICIPANT.id,
                    EntryProfileMoveSourceVisibilityBehaviorContract,
                ),
            ) {
                verifyFeatureContract {
                    var visible = emptySet<Long>()
                    val host = EntryProfileMoveSourceVisibilityHost { _, sourceIds -> visible = sourceIds }
                    host.makeSourcesVisible(2L, setOf(9L))
                    contractExpectation(visible == setOf(9L), "Profile movement must expose moved sources")
                }
            },
        )
    }
}

private fun profileMoveEntry(type: EntryType) = Entry.create().copy(
    id = 83L,
    profileId = 1L,
    source = 9L,
    url = "/entry",
    title = "Entry",
    favorite = true,
    type = type,
)
