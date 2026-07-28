package mihon.entry.interactions

import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryChildListContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryChildListFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(ENTRY_CHILD_LIST_FEATURE_ID, EntryChildListBehaviorContract),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryChildListCapability.definition)
                    val evaluation = productionSubjectEvaluation(
                        EntryChildListCapability.bind(provider),
                        EntryChildListFeatureContributor,
                    )
                    val entry = Entry.create().copy(id = 62L, type = provider.type)
                    val first = EntryChapter.create().copy(id = 91L)
                    val second = EntryChapter.create().copy(id = 92L)
                    val reading = listOf(second, first)
                    val display = listOf(first, second)
                    val feature = DefaultEntryChildListFeature(
                        evaluation = evaluation,
                        childList = object : EntryChildListInteraction {
                            override fun sortedForReading(
                                entry: Entry,
                                chapters: List<EntryChapter>,
                                memberIds: List<Long>,
                            ): List<EntryChapter> = reading

                            override fun sortedForDisplay(
                                entry: Entry,
                                chapters: List<EntryChapter>,
                                memberIds: List<Long>,
                            ): List<EntryChapter> = display
                        },
                        childProgress = object : EntryChildProgressInteraction {
                            override fun progressLabels(
                                request: EntryChildProgressRequest,
                            ): Flow<Map<Long, EntryChildProgressLabel>> = emptyFlow()
                        },
                        missingChildGap = EntryMissingChildGapInteraction {
                            error("Missing-child gaps are not selected by this contract")
                        },
                    )

                    contractExpectation(feature.isApplicable(provider.type), "Child List must be applicable")
                    contractExpectation(
                        feature.readingOrder(entry, display, emptyList()) == EntryChildOrderResult.Available(reading),
                        "Child List must expose reading order",
                    )
                    contractExpectation(
                        feature.firstReadingChild(entry, display, emptyList()) ==
                            EntryFirstChildResult.Available(second),
                        "Child List must derive the first reading child",
                    )
                    contractExpectation(
                        feature.displayOrder(entry, reading, emptyList()) == EntryChildOrderResult.Available(display),
                        "Child List must expose display order",
                    )
                }
            },
        )
    }
}

class EntryChildGroupFilterContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryChildGroupFilterFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.addEntryBackupParticipationContract(
            ENTRY_CHILD_GROUP_FILTER_BACKUP_SNAPSHOT_PARTICIPANT,
            EntryChildGroupFilterBehaviorContract,
            EntryChildGroupFilterBackupState.serializer(),
            EntryChildGroupFilterBackupState(setOf("group")),
        )
        sink.addEntryBackupParticipationContract(
            ENTRY_CHILD_GROUP_FILTER_BACKUP_RESTORE_PARTICIPANT,
            EntryChildGroupFilterBehaviorContract,
            EntryChildGroupFilterBackupState.serializer(),
            EntryChildGroupFilterBackupState(setOf("group")),
        )
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(
                    ENTRY_CHILD_GROUP_FILTER_FEATURE_ID,
                    EntryChildGroupFilterBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryChildGroupFilterCapability.definition)
                    val evaluation = productionSubjectEvaluation(
                        EntryChildGroupFilterCapability.bind(provider),
                        EntryChildGroupFilterFeatureContributor,
                    )
                    val entry = Entry.create().copy(id = 63L, type = provider.type)
                    val visible = EntryChapter.create().copy(id = 93L, scanlator = "visible")
                    val hidden = EntryChapter.create().copy(id = 94L, scanlator = "hidden")
                    val feature = DefaultEntryChildGroupFilterFeature(
                        evaluation = evaluation,
                        interaction = object : EntryChildGroupFilterInteraction {
                            override fun groupFor(entry: Entry, chapter: EntryChapter): String? = chapter.scanlator
                            override fun normalizeGroup(entry: Entry, group: String): String = group.lowercase()
                        },
                        dataSource = mockk(relaxed = true),
                    )

                    contractExpectation(feature.isApplicable(provider.type), "Child Group Filter must be applicable")
                    contractExpectation(
                        feature.filter(entry, listOf(visible, hidden), setOf("HIDDEN")) ==
                            EntryChildGroupFilterResult.Available(listOf(visible)),
                        "Child Group Filter must normalize and exclude matching groups",
                    )
                }
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_CHILD_GROUP_FILTER_PROFILE_MOVE_PARTICIPANT.id,
                    EntryChildGroupFilterProfileMoveBehaviorContract,
                ),
            ) {
                verifyFeatureContract {
                    var moved: EntryProfileMoveStateRequest? = null
                    val host = EntryProfileMoveChildGroupFilterStateHost { request -> moved = request }
                    val request = EntryProfileMoveStateRequest(1L, 2L, listOf(63L))
                    host.move(request)
                    contractExpectation(moved == request, "Profile movement must transfer child-group filter state")
                }
            },
        )
    }
}
