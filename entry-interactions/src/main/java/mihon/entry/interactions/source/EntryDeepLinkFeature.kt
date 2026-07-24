package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryUriType
import eu.kanade.tachiyomi.source.entry.ResolvableSource
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import kotlinx.coroutines.CancellationException
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
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.adapter.toEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.domain.source.service.SourceManager

internal val ENTRY_DEEP_LINK_FEATURE_ID = FeatureId("entry.deep-link")
private val ENTRY_DEEP_LINK_OWNER = ContributionOwner("entry-deep-link")
private val ENTRY_DEEP_LINK_REFERENCE = entryContentTypeReferenceContribution(
    id = "deep-link",
    owner = ENTRY_DEEP_LINK_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Resolve supported links through installed sources",
    order = 700,
    selection = EntryContentTypeReferenceSelection.CONDITIONAL_RELATIONSHIP,
    project = { EntryContentTypeReferenceStatus.SOURCE_DEPENDENT },
)
internal val ENTRY_DEEP_LINK_INTEGRATION_ID = FeatureIntegrationId("entry.deep-link.resolution")

internal object EntryDeepLinkBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.deep-link.behavior")
}

internal enum class EntryDeepLinkMatchState {
    MATCHED,
    NOT_MATCHED,
    FAILED,
}

internal data class EntryDeepLinkContext(
    val hasResolver: Boolean,
    val matchState: EntryDeepLinkMatchState,
)

internal val ENTRY_DEEP_LINK_CONTEXT = entrySourceContextInputDefinition<EntryDeepLinkContext>(
    id = ContextInputId("entry.deep-link.context"),
    contracts = setOf(ResolvableSource::class),
)
private val ENTRY_DEEP_LINK_NO_RESOLVER = FeatureContextBlocker(
    FeatureArtifactId("entry.deep-link.no-resolver"),
    listOf(ENTRY_DEEP_LINK_CONTEXT),
)
private val ENTRY_DEEP_LINK_NO_MATCH = FeatureContextBlocker(
    FeatureArtifactId("entry.deep-link.no-match"),
    listOf(ENTRY_DEEP_LINK_CONTEXT),
)
private enum class EntryDeepLinkBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    SOURCE_DISCOVERY(FeatureArtifactId("entry.deep-link.source-discovery")),
    URI_CLASSIFICATION(FeatureArtifactId("entry.deep-link.uri-classification")),
    ENTRY_RESOLUTION(FeatureArtifactId("entry.deep-link.entry-resolution")),
    PERSISTENCE(FeatureArtifactId("entry.deep-link.persistence")),
    CHILD_RESOLUTION(FeatureArtifactId("entry.deep-link.child-resolution")),
    SOURCE_REFRESH(FeatureArtifactId("entry.deep-link.source-refresh")),
}

internal object EntryDeepLinkFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_DEEP_LINK_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_DEEP_LINK_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_DEEP_LINK_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(ENTRY_DEEP_LINK_CONTEXT),
                        contextRule = featureContextRule(owner) { evidence ->
                            val context = evidence.value(ENTRY_DEEP_LINK_CONTEXT)
                            when {
                                !context.hasResolver ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_DEEP_LINK_NO_RESOLVER))
                                context.matchState == EntryDeepLinkMatchState.NOT_MATCHED ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_DEEP_LINK_NO_MATCH))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(ENTRY_DEEP_LINK_NO_RESOLVER, ENTRY_DEEP_LINK_NO_MATCH),
                        behaviorProjections = EntryDeepLinkBehavior.entries,
                        behavioralContracts = listOf(EntryDeepLinkBehaviorContract),
                        projectionRequirements = listOf(ENTRY_DEEP_LINK_REFERENCE.requirement),
                        projections = listOf(ENTRY_DEEP_LINK_REFERENCE.projection),
                    ),
                ),
            ),
        )
    }
}

internal class DefaultEntryDeepLinkFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val sourceManager: SourceManager,
    private val networkToLocalEntry: NetworkToLocalEntry,
    private val entryChapterRepository: EntryChapterRepository,
    private val sourceRefresh: EntrySourceRefreshFeature,
) : EntryDeepLinkFeature {

    override suspend fun resolve(uri: String): EntryDeepLinkResolution {
        val resolvers = sourceManager.getAll().filterIsInstance<ResolvableSource>()
        if (resolvers.isEmpty()) {
            requireState(
                hasResolver = false,
                matchState = EntryDeepLinkMatchState.NOT_MATCHED,
                applicable = false,
            )
            return EntryDeepLinkResolution.NoMatch
        }

        val match = try {
            resolvers.firstNotNullOfOrNull { source ->
                source.getUriType(uri).takeUnless { it == EntryUriType.Unknown }?.let { source to it }
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            requireState(
                hasResolver = true,
                matchState = EntryDeepLinkMatchState.FAILED,
                applicable = true,
            )
            return EntryDeepLinkResolution.Failed(error)
        }
        requireState(
            hasResolver = true,
            matchState = if (match == null) {
                EntryDeepLinkMatchState.NOT_MATCHED
            } else {
                EntryDeepLinkMatchState.MATCHED
            },
            applicable = match != null,
        )
        match ?: return EntryDeepLinkResolution.NoMatch

        return try {
            resolveMatched(uri, match.first, match.second)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            EntryDeepLinkResolution.Failed(error)
        }
    }

    private suspend fun resolveMatched(
        uri: String,
        source: ResolvableSource,
        uriType: EntryUriType,
    ): EntryDeepLinkResolution {
        val entry = source.getEntry(uri)?.let { networkToLocalEntry(it.toEntry(source.id)) }
            ?: return EntryDeepLinkResolution.NoMatch
        val child = if (uriType == EntryUriType.Chapter) {
            source.getChapter(uri)?.let { getChapter(it, entry) }
        } else {
            null
        }
        return EntryDeepLinkResolution.Resolved(entry, child?.id)
    }

    private suspend fun getChapter(sourceChild: SEntryChapter, entry: Entry): EntryChapter? {
        return entryChapterRepository.getChapterByUrlAndEntryId(sourceChild.url, entry.id)
            ?: when (val refresh = sourceRefresh.refresh(EntrySourceRefreshRequest(entry, manual = false))) {
                is EntrySourceRefreshResult.Refreshed -> {
                    refresh.insertedChildren.find { it.url == sourceChild.url }
                }
                is EntrySourceRefreshResult.SourceUnavailable -> throw SourceNotInstalledException()
                is EntrySourceRefreshResult.Failed -> when (val reason = refresh.reason) {
                    EntrySourceRefreshFailure.NoChildren -> throw NoChaptersException()
                    is EntrySourceRefreshFailure.Operation -> throw reason.error
                }
            }
    }

    private fun requireState(
        hasResolver: Boolean,
        matchState: EntryDeepLinkMatchState,
        applicable: Boolean,
    ) {
        EntryDeepLinkBehavior.entries.forEach { behavior ->
            evaluation.requireSourceContextState(
                feature = ENTRY_DEEP_LINK_FEATURE_ID,
                integration = ENTRY_DEEP_LINK_INTEGRATION_ID,
                behaviorProjection = behavior.id,
                evidence = listOf(
                    contextEvidence(ENTRY_DEEP_LINK_CONTEXT, EntryDeepLinkContext(hasResolver, matchState)),
                ),
                applicable = applicable,
            )
        }
    }
}
