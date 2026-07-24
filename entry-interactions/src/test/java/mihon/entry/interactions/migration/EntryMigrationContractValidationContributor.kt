package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.validation.FeatureContractExecutionInput
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractScenario
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryMigrationContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryMigrationFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        contracts.forEach { item ->
            val reference = FeatureContractReference(ENTRY_MIGRATION_FEATURE_ID, item.contract)
            sink.add(FeatureContractVerifier(reference) { input -> verifyMigration(input, item.contract) })
            item.scenario?.let { scenario ->
                sink.add(
                    FeatureContractScenario(
                        FeatureContractScenarioId("${item.integration.value}.applicable"),
                        reference,
                        item.integration,
                    ) { scenario() },
                )
            }
        }
    }

    private suspend fun verifyMigration(
        input: FeatureContractExecutionInput,
        contract: EntryMigrationBehaviorContract,
    ) = verifyFeatureContract {
        val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
        val source = entry(10L, type, favorite = true).copy(notes = "contract")
        val target = entry(20L, type, favorite = false)
        val sourceChild = EntryChapter.create().copy(id = 11L, entryId = source.id, read = true, bookmark = true)
        val targetChild = EntryChapter.create().copy(id = 21L, entryId = target.id)
        val host = RecordingEntryMigrationHost(
            source,
            target,
            sourceChildren = listOf(sourceChild),
            targetChildren = listOf(targetChild),
        )
        val feature = entryMigrationContractFeature(input, type, host)

        when (contract) {
            EntryMigrationBehaviorContract.PROVIDER,
            EntryMigrationBehaviorContract.SOURCE_CONTEXT,
            -> contractExpectation(
                feature.availability(source) == EntryMigrationAvailability.Available,
                "Migration must expose an applicable source",
            )
            EntryMigrationBehaviorContract.SELECTION_CONTEXT -> contractExpectation(
                feature.prepareSelection(listOf(source, source.copy(id = 12L, url = "entry-12")))
                    is EntryMigrationSelectionResult.Ready,
                "Migration must prepare a same-profile selection",
            )
            else -> {
                val prepared = feature.prepare(EntryMigrationPrepareIntent(source, target))
                contractExpectation(
                    prepared is EntryMigrationPreparationResult.Ready,
                    "Migration must prepare an applicable source-target pair",
                )
                val ready = prepared as EntryMigrationPreparationResult.Ready
                verifyPreparedContract(contract, ready)
                if (contract.requiresExecution) {
                    val selectedOptions = buildSet {
                        if (contract.transfersChildState) add(EntryMigrationOption.CHILD_STATE)
                    }
                    val result = feature.execute(
                        EntryMigrationExecuteIntent(ready.reference, EntryMigrationMode.COPY, selectedOptions),
                    )
                    contractExpectation(
                        result is EntryMigrationExecutionResult.Applied,
                        "Migration must execute its applicable shared workflow",
                    )
                    verifyExecutionContract(contract, host)
                }
            }
        }
    }

    private fun verifyPreparedContract(
        contract: EntryMigrationBehaviorContract,
        ready: EntryMigrationPreparationResult.Ready,
    ) {
        val expectedOption = when (contract) {
            EntryMigrationBehaviorContract.CONSUMPTION,
            EntryMigrationBehaviorContract.BOOKMARK,
            EntryMigrationBehaviorContract.CHILD_STATE_OPTION,
            -> EntryMigrationOption.CHILD_STATE
            EntryMigrationBehaviorContract.CATEGORIES_OPTION -> EntryMigrationOption.CATEGORIES
            EntryMigrationBehaviorContract.NOTES_OPTION -> EntryMigrationOption.NOTES
            EntryMigrationBehaviorContract.CUSTOM_COVER_OPTION -> EntryMigrationOption.CUSTOM_COVER
            else -> null
        }
        if (expectedOption != null) {
            contractExpectation(expectedOption in ready.availableOptions, "Migration must expose $expectedOption")
        }
    }

    private fun verifyExecutionContract(
        contract: EntryMigrationBehaviorContract,
        host: RecordingEntryMigrationHost,
    ) {
        val invoked = when (contract) {
            EntryMigrationBehaviorContract.CONSUMPTION -> host.transitions.single()
                .childUpdates.single().updated.read
            EntryMigrationBehaviorContract.BOOKMARK -> host.transitions.single()
                .childUpdates.single().updated.bookmark
            else -> host.transitions.isNotEmpty()
        }
        contractExpectation(invoked, "Migration must coordinate ${contract.id}")
    }

    private fun entry(id: Long, type: EntryType, favorite: Boolean) = Entry.create().copy(
        id = id,
        profileId = 4L,
        source = id + 100L,
        url = "entry-$id",
        title = "Entry $id",
        favorite = favorite,
        type = type,
    )

    private companion object {
        val contracts = migrationContracts()
    }
}
