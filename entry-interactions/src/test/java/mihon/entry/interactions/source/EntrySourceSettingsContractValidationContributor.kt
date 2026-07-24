package mihon.entry.interactions

import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.entry.ConfigurableSource
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

class EntrySourceSettingsContractValidationContributor : FeatureValidationContributor {
    override val owner = EntrySourceSettingsFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        val reference = FeatureContractReference(SOURCE_SETTINGS_FEATURE_ID, EntrySourceSettingsBehaviorContract)
        sink.add(
            FeatureContractVerifier(reference) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val preferences = mockk<SharedPreferences>()
                    val source = mockk<ConfigurableSource> {
                        every { id } returns 7L
                        every { getSourcePreferences() } returns preferences
                    }
                    val feature = DefaultEntrySourceSettingsFeature(
                        productionSubjectEvaluation(type, EntrySourceSettingsFeatureContributor),
                        mockk { every { get(7L) } returns source },
                    )
                    val result = feature.resolve(7L)
                    contractExpectation(
                        result is EntrySourceSettingsResolution.Available && result.preferences === preferences,
                        "Source Settings must expose applicable source preferences",
                    )
                }
            },
        )
        sink.add(
            FeatureContractScenario(
                FeatureContractScenarioId("entry.source-settings.access.applicable"),
                reference,
                SOURCE_SETTINGS_INTEGRATION_ID,
            ) { listOf(contextEvidence(SOURCE_SETTINGS_CONTEXT, SourceSettingsContext(true, true))) },
        )
    }
}
