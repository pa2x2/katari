package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.ChapterWebViewSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.WebViewSource
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
import tachiyomi.domain.entry.adapter.toSEntry
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

internal val ENTRY_WEB_VIEW_FEATURE_ID = FeatureId("entry.web-view")
private val ENTRY_WEB_VIEW_OWNER = ContributionOwner("entry-web-view")
private val ENTRY_WEB_VIEW_REFERENCE = entryContentTypeReferenceContribution(
    id = "entry-web-view",
    owner = ENTRY_WEB_VIEW_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Open entries in a source WebView",
    order = 1100,
    selection = EntryContentTypeReferenceSelection.CONDITIONAL_RELATIONSHIP,
    project = { EntryContentTypeReferenceStatus.SOURCE_DEPENDENT },
)
private val ENTRY_CHILD_WEB_VIEW_REFERENCE = entryContentTypeReferenceContribution(
    id = "child-web-view",
    owner = ENTRY_WEB_VIEW_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Open child items in a source WebView",
    order = 1200,
    selection = EntryContentTypeReferenceSelection.CONDITIONAL_RELATIONSHIP,
    project = { EntryContentTypeReferenceStatus.SOURCE_DEPENDENT },
)
internal val ENTRY_WEB_VIEW_INTEGRATION_ID = FeatureIntegrationId("entry.web-view.source")
internal val ENTRY_CHILD_WEB_VIEW_INTEGRATION_ID = FeatureIntegrationId("entry.web-view.child-source")

internal object EntryWebViewBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.web-view.behavior")
}

internal object EntryChildWebViewBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.web-view.child-behavior")
}

internal data class EntryWebViewContext(val installed: Boolean, val supported: Boolean)

internal val ENTRY_WEB_VIEW_CONTEXT = entrySourceContextInputDefinition<EntryWebViewContext>(
    id = ContextInputId("entry.web-view.source-context"),
    contracts = setOf(WebViewSource::class),
)
private val ENTRY_WEB_VIEW_MISSING = FeatureContextBlocker(
    FeatureArtifactId("entry.web-view.source-missing"),
    listOf(ENTRY_WEB_VIEW_CONTEXT),
)
private val ENTRY_WEB_VIEW_UNSUPPORTED = FeatureContextBlocker(
    FeatureArtifactId("entry.web-view.unsupported"),
    listOf(ENTRY_WEB_VIEW_CONTEXT),
)
internal data class EntryChildWebViewContext(val installed: Boolean, val supported: Boolean)

internal val ENTRY_CHILD_WEB_VIEW_CONTEXT = entrySourceContextInputDefinition<EntryChildWebViewContext>(
    id = ContextInputId("entry.web-view.child-source-context"),
    contracts = setOf(ChapterWebViewSource::class),
)
private val ENTRY_CHILD_WEB_VIEW_MISSING = FeatureContextBlocker(
    FeatureArtifactId("entry.web-view.child-source-missing"),
    listOf(ENTRY_CHILD_WEB_VIEW_CONTEXT),
)
private val ENTRY_CHILD_WEB_VIEW_UNSUPPORTED = FeatureContextBlocker(
    FeatureArtifactId("entry.web-view.child-unsupported"),
    listOf(ENTRY_CHILD_WEB_VIEW_CONTEXT),
)
private enum class EntryWebViewBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    AVAILABILITY(FeatureArtifactId("entry.web-view.availability")),
    CANONICAL_URL(FeatureArtifactId("entry.web-view.canonical-url")),
    NAVIGATION(FeatureArtifactId("entry.web-view.navigation")),
    SHARE(FeatureArtifactId("entry.web-view.share")),
    ASSIST(FeatureArtifactId("entry.web-view.assist")),
    RUNTIME_HEADERS(FeatureArtifactId("entry.web-view.runtime-headers")),
}
private enum class EntryChildWebViewBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    AVAILABILITY(FeatureArtifactId("entry.web-view.child-availability")),
    CANONICAL_URL(FeatureArtifactId("entry.web-view.child-canonical-url")),
    NAVIGATION(FeatureArtifactId("entry.web-view.child-navigation")),
    BROWSER(FeatureArtifactId("entry.web-view.child-browser")),
    SHARE(FeatureArtifactId("entry.web-view.child-share")),
    ASSIST(FeatureArtifactId("entry.web-view.child-assist")),
}

internal object EntryWebViewFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_WEB_VIEW_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_WEB_VIEW_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_WEB_VIEW_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(ENTRY_WEB_VIEW_CONTEXT),
                        contextRule = featureContextRule(owner) { evidence ->
                            val context = evidence.value(ENTRY_WEB_VIEW_CONTEXT)
                            when {
                                !context.installed -> FeatureContextDecision.Blocked(listOf(ENTRY_WEB_VIEW_MISSING))
                                !context.supported -> FeatureContextDecision.Blocked(listOf(ENTRY_WEB_VIEW_UNSUPPORTED))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(ENTRY_WEB_VIEW_MISSING, ENTRY_WEB_VIEW_UNSUPPORTED),
                        behaviorProjections = EntryWebViewBehavior.entries,
                        behavioralContracts = listOf(EntryWebViewBehaviorContract),
                        projectionRequirements = listOf(ENTRY_WEB_VIEW_REFERENCE.requirement),
                        projections = listOf(ENTRY_WEB_VIEW_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_CHILD_WEB_VIEW_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(ENTRY_CHILD_WEB_VIEW_CONTEXT),
                        contextRule = featureContextRule(owner) { evidence ->
                            val context = evidence.value(ENTRY_CHILD_WEB_VIEW_CONTEXT)
                            when {
                                !context.installed ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_CHILD_WEB_VIEW_MISSING))
                                !context.supported ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_CHILD_WEB_VIEW_UNSUPPORTED))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(
                            ENTRY_CHILD_WEB_VIEW_MISSING,
                            ENTRY_CHILD_WEB_VIEW_UNSUPPORTED,
                        ),
                        specializedPrerequisites = listOf(EntryChildWebViewHostContribution.definition),
                        behaviorProjections = EntryChildWebViewBehavior.entries,
                        behavioralContracts = listOf(EntryChildWebViewBehaviorContract),
                        projectionRequirements = listOf(ENTRY_CHILD_WEB_VIEW_REFERENCE.requirement),
                        projections = listOf(ENTRY_CHILD_WEB_VIEW_REFERENCE.projection),
                    ),
                ),
            ),
        )
    }
}

internal class DefaultEntryWebViewFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val sourceManager: SourceManager,
) : EntryWebViewFeature {

    override fun resolveEntry(entry: Entry): EntryWebViewResolution {
        val source = sourceManager.get(entry.source)
        val provider = source as? WebViewSource
        requireState(source != null, provider != null)
        if (source == null) return EntryWebViewResolution.Missing(entry.source)
        if (provider == null) return EntryWebViewResolution.Unsupported(entry.source)

        return runCatching {
            EntryWebViewResolution.Available(
                sourceId = source.id,
                url = provider.getContentUrl(entry.toSEntry()),
                headers = provider.getWebViewHeaders(),
            )
        }.getOrElse { EntryWebViewResolution.Failed(entry.source, it) }
    }

    override fun resolveChild(owner: Entry, child: EntryChapter): EntryChildWebViewResolution {
        val source = sourceManager.get(owner.source)
        val provider = source as? ChapterWebViewSource
        requireChildState(owner.type, source != null, provider != null)
        if (source == null) return EntryChildWebViewResolution.Missing(owner.source)
        if (provider == null) return EntryChildWebViewResolution.Unsupported(owner.source)

        return runCatching {
            EntryChildWebViewResolution.Available(
                sourceId = owner.source,
                url = provider.getChapterUrl(child.toSEntryChapter()),
            )
        }.getOrElse { EntryChildWebViewResolution.Failed(owner.source, it) }
    }

    override fun resolveHeaders(sourceId: Long): EntryWebViewHeadersResolution {
        val source = sourceManager.get(sourceId)
        val provider = source as? WebViewSource
        requireState(source != null, provider != null)
        if (source == null) return EntryWebViewHeadersResolution.Missing
        if (provider == null) return EntryWebViewHeadersResolution.Unsupported
        return runCatching { EntryWebViewHeadersResolution.Available(provider.getWebViewHeaders()) }
            .getOrElse(EntryWebViewHeadersResolution::Failed)
    }

    private fun requireState(installed: Boolean, supported: Boolean) {
        EntryWebViewBehavior.entries.forEach { behavior ->
            evaluation.requireSourceContextState(
                feature = ENTRY_WEB_VIEW_FEATURE_ID,
                integration = ENTRY_WEB_VIEW_INTEGRATION_ID,
                behaviorProjection = behavior.id,
                evidence = listOf(contextEvidence(ENTRY_WEB_VIEW_CONTEXT, EntryWebViewContext(installed, supported))),
                applicable = installed && supported,
            )
        }
    }

    private fun requireChildState(ownerType: EntryType, installed: Boolean, supported: Boolean) {
        EntryChildWebViewBehavior.entries.forEach { behavior ->
            evaluation.requireSourceContextState(
                feature = ENTRY_WEB_VIEW_FEATURE_ID,
                integration = ENTRY_CHILD_WEB_VIEW_INTEGRATION_ID,
                behaviorProjection = behavior.id,
                evidence = listOf(
                    contextEvidence(
                        ENTRY_CHILD_WEB_VIEW_CONTEXT,
                        EntryChildWebViewContext(installed, supported),
                    ),
                ),
                applicable = installed && supported,
                contentType = ownerType,
            )
        }
    }
}
