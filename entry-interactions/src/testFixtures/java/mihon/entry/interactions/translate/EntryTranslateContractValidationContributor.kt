package mihon.entry.interactions.translate

import io.mockk.mockk
import mihon.entry.interactions.download.EntryDownloadCapability
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry

class EntryTranslateContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryTranslateFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(ENTRY_TRANSLATE_FEATURE_ID, EntryTranslateBehaviorContract),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryTranslateCapability.definition)
                    val evaluation = productionSubjectEvaluation(
                        listOf(
                            EntryTranslateCapability.bind(provider),
                            EntryDownloadCapability.bind(input.provider(EntryDownloadCapability.definition)),
                        ),
                        EntryTranslateFeatureContributor,
                    )
                    val feature = DefaultEntryTranslateFeature(
                        evaluation = evaluation,
                        repository = mockk(relaxed = true),
                        translate = mockk(relaxed = true),
                        languages = mockk(relaxed = true),
                        download = mockk(relaxed = true),
                        entries = mockk(relaxed = true),
                        chapters = mockk(relaxed = true),
                        runner = mockk(relaxed = true),
                        work = mockk(relaxed = true),
                    )
                    val entry = Entry.create().copy(id = 71L, type = provider.type)

                    contractExpectation(feature.isApplicable(provider.type), "Translate must be applicable")
                    contractExpectation(
                        provider.translatedChapters(entry, emptyList()).isEmpty(),
                        "Translate providers must report only chapters they were asked about",
                    )
                }
            },
        )
    }
}
