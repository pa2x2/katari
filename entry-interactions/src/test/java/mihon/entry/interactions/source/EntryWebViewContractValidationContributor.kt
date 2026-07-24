package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.ChapterWebViewSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.WebViewSource
import io.mockk.every
import io.mockk.mockk
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractScenario
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryWebViewContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryWebViewFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        val entryReference = FeatureContractReference(ENTRY_WEB_VIEW_FEATURE_ID, EntryWebViewBehaviorContract)
        sink.add(FeatureContractVerifier(entryReference) { input -> verifyEntry(input) })
        sink.add(
            FeatureContractScenario(
                FeatureContractScenarioId("entry.web-view.source.applicable"),
                entryReference,
                ENTRY_WEB_VIEW_INTEGRATION_ID,
            ) { listOf(contextEvidence(ENTRY_WEB_VIEW_CONTEXT, EntryWebViewContext(true, true))) },
        )

        val childReference = FeatureContractReference(ENTRY_WEB_VIEW_FEATURE_ID, EntryChildWebViewBehaviorContract)
        sink.add(FeatureContractVerifier(childReference) { input -> verifyChild(input) })
        sink.add(
            FeatureContractScenario(
                FeatureContractScenarioId("entry.web-view.child-source.applicable"),
                childReference,
                ENTRY_CHILD_WEB_VIEW_INTEGRATION_ID,
            ) { listOf(contextEvidence(ENTRY_CHILD_WEB_VIEW_CONTEXT, EntryChildWebViewContext(true, true))) },
        )
    }

    private suspend fun verifyEntry(input: mihon.feature.graph.validation.FeatureContractExecutionInput) =
        verifyFeatureContract {
            val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
            val source = mockk<WebViewSource> {
                every { id } returns 7L
                every { getContentUrl(any()) } returns "https://example.test/entry"
                every { getWebViewHeaders() } returns mapOf("Contract" to "source")
            }
            val feature = DefaultEntryWebViewFeature(
                productionSubjectEvaluation(type, EntryWebViewFeatureContributor),
                mockk { every { get(7L) } returns source },
            )
            contractExpectation(
                feature.resolveEntry(Entry.create().copy(source = 7L, type = type)) ==
                    EntryWebViewResolution.Available(
                        7L,
                        "https://example.test/entry",
                        mapOf("Contract" to "source"),
                    ),
                "WebView must expose the canonical entry URL and runtime headers",
            )
        }

    private suspend fun verifyChild(input: mihon.feature.graph.validation.FeatureContractExecutionInput) =
        verifyFeatureContract {
            val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
            val source = mockk<ChapterWebViewSource> {
                every { getChapterUrl(any()) } returns "https://example.test/child"
            }
            val host = EntryChildWebViewHostContribution.bind(
                input.adapter(EntryChildWebViewHostContribution.definition),
            )
            val feature = DefaultEntryWebViewFeature(
                productionSubjectEvaluation(type, EntryWebViewFeatureContributor, listOf(host)),
                mockk { every { get(7L) } returns source },
            )
            contractExpectation(
                feature.resolveChild(
                    Entry.create().copy(source = 7L, type = type),
                    EntryChapter.create(),
                ) == EntryChildWebViewResolution.Available(7L, "https://example.test/child"),
                "Child WebView must resolve through an enrolled host adapter",
            )
        }
}
