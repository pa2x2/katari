package eu.kanade.tachiyomi.documentation

import eu.kanade.domain.track.service.GlobalTrackPreferences
import eu.kanade.presentation.more.settings.screen.productionEntryViewerSettingsScreenProjectionResolver
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.adapter.LEGACY_MANGA_SOURCE_SUPPORTED_ENTRY_TYPES
import io.mockk.mockk
import mihon.entry.interactions.documentation.EntryContentTypeReferenceRegistrations
import mihon.entry.interactions.documentation.entryContentTypeReferenceContextEvidence
import mihon.entry.interactions.documentation.projection.EntryContentTypeReferencePlan
import mihon.entry.interactions.documentation.projection.planEntryContentTypeReference
import mihon.entry.interactions.documentation.source.EntrySourceSdkConsumerCoveragePlan
import mihon.entry.interactions.documentation.source.planEntrySourceSdkConsumerCoverage
import mihon.entry.interactions.toContentTypeId
import mihon.entry.interactions.validation.ProductionEntryInteractionValidationEnvironment
import tachiyomi.source.local.LOCAL_SOURCE_SUPPORTED_ENTRY_TYPES
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingletonFactory
import java.io.File

class ProductionEntryDocumentationEnvironment(
    temporaryDirectory: File,
) : AutoCloseable {
    private val interactionEnvironment = ProductionEntryInteractionValidationEnvironment(
        temporaryDirectory,
        productionEntryViewerSettingsScreenProjectionResolver(),
    )

    fun contentTypeReferencePlan(): EntryContentTypeReferencePlan {
        val composition = interactionEnvironment.composition()
        Injekt.addSingletonFactory<GlobalTrackPreferences> { mockk(relaxed = true) }
        val registrations = EntryContentTypeReferenceRegistrations(
            localSource = LOCAL_SOURCE_SUPPORTED_ENTRY_TYPES.mapTo(mutableSetOf()) { it.toContentTypeId() },
            legacyExtensions = LEGACY_MANGA_SOURCE_SUPPORTED_ENTRY_TYPES.mapTo(mutableSetOf()) {
                it.toContentTypeId()
            },
            trackingServices = TrackerManager().trackers
                .flatMapTo(mutableSetOf()) { tracker ->
                    tracker.supportedEntryTypes.map { it.toContentTypeId() }
                },
        )
        return planEntryContentTypeReference(
            graph = composition.featureGraph,
            evaluation = composition.featureGraphEvaluation,
            contextEvidence = entryContentTypeReferenceContextEvidence(registrations),
        )
    }

    fun sourceSdkConsumerCoveragePlan(): EntrySourceSdkConsumerCoveragePlan {
        return planEntrySourceSdkConsumerCoverage(interactionEnvironment.composition().featureGraph)
    }

    override fun close() {
        interactionEnvironment.close()
    }
}
