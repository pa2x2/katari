package mihon.entry.interactions

import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegrationId

internal val ENTRY_TRACKING_FEATURE_ID = FeatureId("entry.tracking")
internal val ENTRY_TRACKING_OWNER = ContributionOwner("entry-tracking")
internal val ENTRY_TRACKING_REFERENCE = entryContentTypeReferenceContribution(
    id = "tracking",
    owner = ENTRY_TRACKING_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Connect entries to tracking services",
    order = 500,
)

internal enum class EntryTrackingIntegration(
    val id: FeatureIntegrationId,
) {
    REGISTRY(FeatureIntegrationId("entry.tracking.registry")),
    AVAILABILITY(FeatureIntegrationId("entry.tracking.availability")),
    SESSION(FeatureIntegrationId("entry.tracking.session")),
    AUTOMATIC_BINDING(FeatureIntegrationId("entry.tracking.automatic-binding")),
    SYNCHRONIZATION(FeatureIntegrationId("entry.tracking.synchronization")),
    MIGRATION_PREPARATION(FeatureIntegrationId("entry.tracking.migration-preparation")),
    LIBRARY(FeatureIntegrationId("entry.tracking.library")),
    STATS(FeatureIntegrationId("entry.tracking.stats")),
}

internal enum class EntryTrackingBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    SERVICE_REGISTRY(FeatureArtifactId("entry.tracking.service-registry")),
    SERVICE_PRESENTATION(FeatureArtifactId("entry.tracking.service-presentation")),
    ACCOUNT_SETTINGS(FeatureArtifactId("entry.tracking.account-settings")),
    BACKUP_DIAGNOSTICS(FeatureArtifactId("entry.tracking.backup-diagnostics")),
    ENTRY_ACTION(FeatureArtifactId("entry.tracking.entry-action")),
    DOCUMENTATION(FeatureArtifactId("entry.tracking.documentation")),
    ENTRY_SESSION(FeatureArtifactId("entry.tracking.entry-session")),
    ENTRY_OPERATIONS(FeatureArtifactId("entry.tracking.entry-operations")),
    AUTOMATIC_BINDING(FeatureArtifactId("entry.tracking.automatic-binding")),
    PROGRESS_SYNCHRONIZATION(FeatureArtifactId("entry.tracking.progress-synchronization")),
    MIGRATION_PREPARATION(FeatureArtifactId("entry.tracking.migration-preparation")),
    LIBRARY_FILTER_EVIDENCE(FeatureArtifactId("entry.tracking.library-filter-evidence")),
    LIBRARY_SCORE_EVIDENCE(FeatureArtifactId("entry.tracking.library-score-evidence")),
    STATS_EVIDENCE(FeatureArtifactId("entry.tracking.stats-evidence")),
}

internal enum class EntryTrackingBehaviorContract(
    override val id: FeatureArtifactId,
) : FeatureBehaviorContract {
    REGISTRY(FeatureArtifactId("entry.tracking.behavior")),
    AVAILABILITY(FeatureArtifactId("entry.tracking.availability.behavior")),
    SESSION(FeatureArtifactId("entry.tracking.session.behavior")),
    AUTOMATIC_BINDING(FeatureArtifactId("entry.tracking.automatic-binding.behavior")),
    SYNCHRONIZATION(FeatureArtifactId("entry.tracking.synchronization.behavior")),
    MIGRATION_PREPARATION(FeatureArtifactId("entry.tracking.migration-preparation.behavior")),
    LIBRARY(FeatureArtifactId("entry.tracking.library.behavior")),
    STATS(FeatureArtifactId("entry.tracking.stats.behavior")),
}
