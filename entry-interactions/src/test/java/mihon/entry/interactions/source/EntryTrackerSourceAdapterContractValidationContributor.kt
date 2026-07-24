package mihon.entry.interactions

import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.EntryType
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
import okhttp3.OkHttpClient

class EntryTrackerSourceAdapterContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryTrackerSourceAdapterFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        val reference = FeatureContractReference(
            TRACKER_SOURCE_ADAPTER_FEATURE_ID,
            EntryTrackerSourceAdapterBehaviorContract,
        )
        sink.add(
            FeatureContractVerifier(reference) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val preferences = mockk<SharedPreferences>()
                    val client = mockk<OkHttpClient>()
                    val source = mockk<EntryImageSource> { every { this@mockk.client } returns client }
                    val settings = mockk<EntrySourceSettingsFeature> {
                        every { resolve(7L) } returns EntrySourceSettingsResolution.Available(7L, preferences) {}
                    }
                    val home = mockk<EntrySourceHomeFeature> {
                        every { resolve(7L) } returns EntrySourceHomeResolution.Available(7L, "Source", "https://home")
                    }
                    val feature = DefaultEntryTrackerSourceAdapterFeature(
                        productionSubjectEvaluation(type, EntryTrackerSourceAdapterFeatureContributor),
                        mockk { every { get(7L) } returns source },
                        settings,
                        home,
                    )
                    contractExpectation(
                        feature.resolve(7L) ==
                            EntryTrackerSourceAdapterResolution.Available(7L, preferences, "https://home", client),
                        "Tracker Source Adapter must compose its available source relationships",
                    )
                }
            },
        )
        sink.add(
            FeatureContractScenario(
                FeatureContractScenarioId("entry.tracker-source-adapter.connection.applicable"),
                reference,
                TRACKER_SOURCE_ADAPTER_INTEGRATION_ID,
            ) {
                listOf(
                    contextEvidence(TRACKER_SOURCE_SETTINGS_CONTEXT, true),
                    contextEvidence(TRACKER_SOURCE_HOME_CONTEXT, true),
                    contextEvidence(TRACKER_SOURCE_IMAGE_CLIENT_CONTEXT, true),
                )
            },
        )
    }
}
