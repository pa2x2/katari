package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.RelatedEntriesSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSelection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceStatus
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.entry.interactions.documentation.source.entrySourceContextInputDefinition
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule
import tachiyomi.domain.entry.adapter.toEntry
import tachiyomi.domain.entry.adapter.toSEntry
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.identity
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort
import tachiyomi.domain.source.service.SourceManager

internal val ENTRY_RELATED_ENTRIES_FEATURE_ID = FeatureId("entry.related-entries")
internal val ENTRY_RELATED_ENTRIES_INTEGRATION_ID = FeatureIntegrationId("entry.related-entries.source-context")
private val ENTRY_RELATED_ENTRIES_FEATURE_OWNER = ContributionOwner("entry-related-entries")
private val ENTRY_RELATED_ENTRIES_REFERENCE = entryContentTypeReferenceContribution(
    id = "related-entries",
    owner = ENTRY_RELATED_ENTRIES_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Discover related entries from a source",
    order = 250,
    selection = EntryContentTypeReferenceSelection.CONDITIONAL_RELATIONSHIP,
    project = { EntryContentTypeReferenceStatus.SOURCE_DEPENDENT },
)

private enum class EntryRelatedEntriesBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    AVAILABILITY(FeatureArtifactId("entry.related-entries.availability")),
    FETCH(FeatureArtifactId("entry.related-entries.fetch")),
    PERSISTENCE(FeatureArtifactId("entry.related-entries.persistence")),
    LIBRARY_STATE(FeatureArtifactId("entry.related-entries.library-state")),
    ORIENTATION(FeatureArtifactId("entry.related-entries.orientation")),
    ENTRY_SURFACE(FeatureArtifactId("entry.related-entries.entry-surface")),
    DETAILS_NAVIGATION(FeatureArtifactId("entry.related-entries.details-navigation")),
}

internal object EntryRelatedEntriesBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.related-entries.behavior")
}

internal val ENTRY_RELATED_ENTRIES_SOURCE_INSTALLED_CONTEXT = entrySourceContextInputDefinition<Boolean>(
    id = ContextInputId("entry.related-entries.source-installed"),
    nonContractReason = "Source installation is runtime registration state, not a public SDK contract",
)
internal val ENTRY_RELATED_ENTRIES_SOURCE_SUPPORT_CONTEXT = entrySourceContextInputDefinition<Boolean>(
    id = ContextInputId("entry.related-entries.source-support"),
    contracts = setOf(RelatedEntriesSource::class),
)
private val ENTRY_RELATED_ENTRIES_SOURCE_MISSING = FeatureContextBlocker(
    FeatureArtifactId("entry.related-entries.source-missing"),
    listOf(ENTRY_RELATED_ENTRIES_SOURCE_INSTALLED_CONTEXT),
)
private val ENTRY_RELATED_ENTRIES_SOURCE_UNSUPPORTED = FeatureContextBlocker(
    FeatureArtifactId("entry.related-entries.source-unsupported"),
    listOf(ENTRY_RELATED_ENTRIES_SOURCE_SUPPORT_CONTEXT),
)

internal object EntryRelatedEntriesFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_RELATED_ENTRIES_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_RELATED_ENTRIES_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_RELATED_ENTRIES_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(
                            ENTRY_RELATED_ENTRIES_SOURCE_INSTALLED_CONTEXT,
                            ENTRY_RELATED_ENTRIES_SOURCE_SUPPORT_CONTEXT,
                        ),
                        contextRule = featureContextRule(owner) { evidence ->
                            when {
                                !evidence.value(ENTRY_RELATED_ENTRIES_SOURCE_INSTALLED_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_RELATED_ENTRIES_SOURCE_MISSING))
                                !evidence.value(ENTRY_RELATED_ENTRIES_SOURCE_SUPPORT_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_RELATED_ENTRIES_SOURCE_UNSUPPORTED))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(
                            ENTRY_RELATED_ENTRIES_SOURCE_MISSING,
                            ENTRY_RELATED_ENTRIES_SOURCE_UNSUPPORTED,
                        ),
                        behaviorProjections = EntryRelatedEntriesBehavior.entries,
                        behavioralContracts = listOf(EntryRelatedEntriesBehaviorContract),
                        projectionRequirements = listOf(ENTRY_RELATED_ENTRIES_REFERENCE.requirement),
                        projections = listOf(ENTRY_RELATED_ENTRIES_REFERENCE.projection),
                    ),
                ),
            ),
        )
    }
}

internal class DefaultEntryRelatedEntriesFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val sourceManager: SourceManager,
    private val networkToLocalEntry: NetworkToLocalEntry,
    private val getEntry: GetEntry,
    private val sourceDescription: EntrySourceDescriptionResolutionPort,
) : EntryRelatedEntriesFeature {

    override fun availability(context: EntryRelatedEntriesContext): EntryRelatedEntriesAvailability {
        val source = context.source
        if (source != null) {
            check(source.id == context.entry.source) {
                "Related Entries context source ${source.id} does not own Entry source ${context.entry.source}"
            }
        }
        return availability(context.entry.type, source)
    }

    override suspend fun load(entryId: Long): EntryRelatedEntriesLoadResult {
        val entry = getEntry.await(entryId) ?: error("Entry $entryId was not found")
        val source = sourceManager.get(entry.source)
        val availability = availability(entry.type, source)
        if (availability is EntryRelatedEntriesAvailability.Unavailable) {
            return EntryRelatedEntriesLoadResult.Unavailable(availability.reason)
        }
        val orientation = (availability as EntryRelatedEntriesAvailability.Available).orientation
        val relatedSource = source as RelatedEntriesSource
        val entries = relatedSource.getRelatedEntries(entry.toSEntry())
            .map { it.toEntry(source.id) }
            .distinctBy(Entry::identity)
            .let { networkToLocalEntry(it) }
        return EntryRelatedEntriesLoadResult.Loaded(entries, orientation)
    }

    override fun observeEntry(entry: Entry): Flow<Entry> {
        return getEntry.subscribe(entry.url, entry.source, entry.type).filterNotNull()
    }

    private fun availability(type: EntryType, source: UnifiedSource?): EntryRelatedEntriesAvailability {
        val installed = source != null
        val supported = source is RelatedEntriesSource
        evaluation.requireEntryContextState(
            type = type,
            feature = ENTRY_RELATED_ENTRIES_FEATURE_ID,
            integration = ENTRY_RELATED_ENTRIES_INTEGRATION_ID,
            behaviorProjections = EntryRelatedEntriesBehavior.entries.map(EntryRelatedEntriesBehavior::id),
            evidence = listOf(
                contextEvidence(ENTRY_RELATED_ENTRIES_SOURCE_INSTALLED_CONTEXT, installed),
                contextEvidence(ENTRY_RELATED_ENTRIES_SOURCE_SUPPORT_CONTEXT, supported),
            ),
            applicable = installed && supported,
        )
        return if (source is RelatedEntriesSource) {
            EntryRelatedEntriesAvailability.Available(sourceDescription.describe(source).itemOrientation)
        } else {
            EntryRelatedEntriesAvailability.Unavailable(
                if (source == null) {
                    EntryRelatedEntriesUnavailableReason.SOURCE_MISSING
                } else {
                    EntryRelatedEntriesUnavailableReason.SOURCE_UNSUPPORTED
                },
            )
        }
    }
}
