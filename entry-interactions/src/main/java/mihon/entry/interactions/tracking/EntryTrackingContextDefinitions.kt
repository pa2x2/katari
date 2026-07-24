package mihon.entry.interactions

import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.contextInputDefinition

private val TRACKER_SYSTEM_OWNER = ContributionOwner("entry-tracker-system")

internal val ENTRY_TRACKING_REGISTERED_SUPPORT = contextInputDefinition<Boolean>(
    ContextInputId("entry.tracking.registered-support"),
    TRACKER_SYSTEM_OWNER,
)
internal val ENTRY_TRACKING_AUTHENTICATED_SUPPORT = contextInputDefinition<Boolean>(
    ContextInputId("entry.tracking.authenticated-support"),
    TRACKER_SYSTEM_OWNER,
)
internal val ENTRY_TRACKING_SOURCE_ACCEPTED = contextInputDefinition<Boolean>(
    ContextInputId("entry.tracking.source-accepted"),
    TRACKER_SYSTEM_OWNER,
)
internal val ENTRY_TRACKING_AUTOMATIC_SOURCE_ACCEPTED = contextInputDefinition<Boolean>(
    ContextInputId("entry.tracking.automatic-source-accepted"),
    TRACKER_SYSTEM_OWNER,
)
internal val ENTRY_TRACKING_AUTHENTICATED_TRACK = contextInputDefinition<Boolean>(
    ContextInputId("entry.tracking.authenticated-track"),
    TRACKER_SYSTEM_OWNER,
)

internal val NO_REGISTERED_TRACKER = FeatureContextBlocker(
    FeatureArtifactId("entry.tracking.no-registered-tracker"),
    listOf(ENTRY_TRACKING_REGISTERED_SUPPORT),
)
internal val NO_AUTHENTICATED_TRACKER = FeatureContextBlocker(
    FeatureArtifactId("entry.tracking.no-authenticated-tracker"),
    listOf(ENTRY_TRACKING_AUTHENTICATED_SUPPORT),
)
internal val SOURCE_NOT_ACCEPTED = FeatureContextBlocker(
    FeatureArtifactId("entry.tracking.source-not-accepted"),
    listOf(ENTRY_TRACKING_SOURCE_ACCEPTED),
)
internal val NO_AUTOMATIC_TRACKER = FeatureContextBlocker(
    FeatureArtifactId("entry.tracking.no-automatic-tracker"),
    listOf(ENTRY_TRACKING_AUTOMATIC_SOURCE_ACCEPTED),
)
internal val NO_AUTHENTICATED_TRACK = FeatureContextBlocker(
    FeatureArtifactId("entry.tracking.no-authenticated-track"),
    listOf(ENTRY_TRACKING_AUTHENTICATED_TRACK),
)
