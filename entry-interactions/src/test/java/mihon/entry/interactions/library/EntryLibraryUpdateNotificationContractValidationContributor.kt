package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.every
import io.mockk.mockk
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.validation.FeatureContractExecutionInput
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractScenario
import mihon.feature.graph.validation.FeatureContractVerificationResult
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

class EntryLibraryUpdateNotificationContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryLibraryUpdateNotificationFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        notificationContracts.forEach { item ->
            val reference = FeatureContractReference(ENTRY_LIBRARY_UPDATE_NOTIFICATION_FEATURE_ID, item.contract)
            sink.add(FeatureContractVerifier(reference, ::verifyLibraryUpdateNotification))
            if (item.integration.isContextualNotificationAction()) {
                sink.add(
                    FeatureContractScenario(
                        FeatureContractScenarioId("${item.integration.value}.applicable"),
                        reference,
                        item.integration,
                    ) { listOf(contextEvidence(ENTRY_LIBRARY_UPDATE_NOTIFICATION_HAS_CHILDREN_CONTEXT, true)) },
                )
            }
        }
    }
}

private data class NotificationContract(
    val integration: FeatureIntegrationId,
    val contract: FeatureBehaviorContract,
)

private val notificationContracts = listOf(
    NotificationContract(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_BASE_INTEGRATION_ID,
        EntryLibraryUpdateNotificationBehaviorContract,
    ),
    NotificationContract(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_PRESENTATION_INTEGRATION_ID,
        EntryLibraryUpdateNotificationPresentationBehaviorContract,
    ),
    NotificationContract(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_INTEGRATION_ID,
        EntryLibraryUpdateNotificationOpenBehaviorContract,
    ),
    NotificationContract(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_CONTEXT_INTEGRATION_ID,
        EntryLibraryUpdateNotificationOpenActionBehaviorContract,
    ),
    NotificationContract(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_INTEGRATION_ID,
        EntryLibraryUpdateNotificationConsumptionBehaviorContract,
    ),
    NotificationContract(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_CONTEXT_INTEGRATION_ID,
        EntryLibraryUpdateNotificationConsumptionActionBehaviorContract,
    ),
    NotificationContract(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_DOWNLOAD_INTEGRATION_ID,
        EntryLibraryUpdateNotificationDownloadBehaviorContract,
    ),
)

private suspend fun verifyLibraryUpdateNotification(
    input: FeatureContractExecutionInput,
): FeatureContractVerificationResult = verifyFeatureContract {
    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
    val integration = input.subject.integration
    val bindings = when (integration) {
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_PRESENTATION_INTEGRATION_ID -> listOf(
            EntryTypePresentationCapability.bind(input.provider(EntryTypePresentationCapability.definition)),
        )
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_INTEGRATION_ID,
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_CONTEXT_INTEGRATION_ID,
        -> listOf(EntryOpenCapability.bind(input.provider(EntryOpenCapability.definition)))
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_INTEGRATION_ID,
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_CONTEXT_INTEGRATION_ID,
        -> listOf(EntryConsumptionCapability.bind(input.provider(EntryConsumptionCapability.definition)))
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_DOWNLOAD_INTEGRATION_ID -> listOf(
            EntryDownloadCapability.bind(input.provider(EntryDownloadCapability.definition)),
        )
        else -> emptyList()
    }
    val evaluation = if (bindings.isEmpty()) {
        productionSubjectEvaluation(type, EntryLibraryUpdateNotificationFeatureContributor)
    } else {
        productionSubjectEvaluation(bindings, EntryLibraryUpdateNotificationFeatureContributor)
    }
    val presentationProvider = bindings.map { it.implementation }.filterIsInstance<EntryTypePresentationProvider>()
        .singleOrNull()
    val presentationFeature = mockk<EntryTypePresentationFeature> {
        every { genericPresentation } returns genericEntryTypePresentation
        every { presentation(type) } returns if (presentationProvider == null) {
            EntryTypePresentationResult.Generic(type, genericEntryTypePresentation)
        } else {
            EntryTypePresentationResult.Contributed(type, presentationProvider.presentation)
        }
    }
    val feature = DefaultEntryLibraryUpdateNotificationFeature(
        evaluation = evaluation,
        presentationFeature = presentationFeature,
        openFeature = mockk { every { isApplicable(type) } returns true },
        consumptionFeature = mockk { every { isApplicable(type) } returns true },
        downloadActionFeature = mockk {
            every { notificationAvailability(any(), any()) } returns EntryDownloadActionAvailability.Available
        },
        sourceManager = mockk<SourceManager>(relaxed = true),
        resolveVisibleEntry = { it },
    )
    val entry = Entry.create().copy(id = 88L, type = type)
    val child = EntryChapter.create().copy(id = 89L, entryId = entry.id)
    val item = feature.project(
        listOf(EntryLibraryUpdateNotificationInput(entry, listOf(child))),
    ).groups.single().updates.single()

    contractExpectation(
        EntryLibraryUpdateNotificationAction.VIEW_ENTRY in item.actions,
        "Library Update Notifications must retain the base view action",
    )
    when (integration) {
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_INTEGRATION_ID,
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_CONTEXT_INTEGRATION_ID,
        -> contractExpectation(
            item.destination == EntryLibraryUpdateNotificationDestination.OPEN_CHILD,
            "Library Update Notifications must derive the Open destination",
        )
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_INTEGRATION_ID,
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_CONTEXT_INTEGRATION_ID,
        -> contractExpectation(
            EntryLibraryUpdateNotificationAction.MARK_CONSUMED in item.actions,
            "Library Update Notifications must derive the Consumption action",
        )
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_DOWNLOAD_INTEGRATION_ID -> contractExpectation(
            EntryLibraryUpdateNotificationAction.DOWNLOAD in item.actions,
            "Library Update Notifications must derive the Download action",
        )
        else -> Unit
    }
}

private fun FeatureIntegrationId.isContextualNotificationAction(): Boolean = this in setOf(
    ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_CONTEXT_INTEGRATION_ID,
    ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_CONTEXT_INTEGRATION_ID,
)
