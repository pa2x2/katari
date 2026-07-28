package mihon.entry.interactions

import dev.icerock.moko.resources.StringResource
import io.mockk.mockk
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class EntryMediaCacheContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryMediaCacheFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(ENTRY_MEDIA_CACHE_FEATURE_ID, EntryMediaCacheBehaviorContract),
            ) { input ->
                verifyFeatureContract {
                    val selected = input.provider(EntryMediaCacheCapability.definition)
                    val evaluation = productionSubjectEvaluation(
                        EntryMediaCacheCapability.bind(selected),
                        EntryMediaCacheFeatureContributor,
                    )
                    val artifact = RecordingContractMediaCacheArtifact()
                    val provider = object : EntryMediaCacheProvider {
                        override val type = selected.type
                        override val artifacts = listOf(artifact)
                    }
                    val feature = DefaultEntryMediaCacheFeature(
                        evaluation,
                        object : EntryMediaCacheInteraction {
                            override fun provider(type: eu.kanade.tachiyomi.source.entry.EntryType) =
                                provider.takeIf { type == provider.type }
                        },
                        InMemoryPreferenceStore(),
                    )

                    contractExpectation(
                        feature.settings().single().id == artifact.id,
                        "Media Cache must expose provider settings",
                    )
                    contractExpectation(
                        feature.clear(artifact.id) is EntryMediaCacheClearResult.Cleared && artifact.clearCount == 1,
                        "Media Cache must dispatch manual clearing",
                    )
                }
            },
        )
    }
}

private class RecordingContractMediaCacheArtifact : EntryMediaCacheArtifact {
    override val id = EntryMediaCacheId("contract.cache")
    override val clearLabel: StringResource = mockk()
    override val autoClearLabel: StringResource = mockk()
    override val readableSize = "0 B"
    override val autoClearPreference = EntryMediaCacheAutoClearPreference("contract_cache_auto_clear")
    var clearCount = 0

    override fun clear(): Int {
        clearCount++
        return 1
    }
}
