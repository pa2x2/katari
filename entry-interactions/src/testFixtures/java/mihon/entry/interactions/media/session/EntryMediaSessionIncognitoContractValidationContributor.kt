package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.mockk
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.repository.EntryRepository

class EntryMediaSessionIncognitoContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryMediaSessionIncognitoContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(
                    ENTRY_MEDIA_SESSION_INCOGNITO_FEATURE_ID,
                    EntryMediaSessionIncognitoBehaviorContract,
                ),
            ) { input ->
                verifyIncognito(input.provider(EntryMediaSessionCapability.definition).type)
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_MEDIA_SESSION_INCOGNITO_PARTICIPANT.id,
                    EntryMediaSessionIncognitoBehaviorContract,
                ),
            ) { input ->
                verifyIncognito(input.provider(EntryMediaSessionCapability.definition).type)
            },
        )
    }

    private suspend fun verifyIncognito(type: EntryType) = verifyFeatureContract {
        val event = mediaSessionContractEvent(type)
        val execution = EntryMediaSessionExecutionEvent(event)
        val repository = mockk<EntryRepository> {
            coEvery { getEntryById(event.child.entryId) } returns event.visibleEntry
        }
        entryMediaSessionIncognitoBinding(
            repository = { repository },
            incognitoState = EntryMediaSessionIncognitoState { true },
        ).handler.execute(execution)

        contractExpectation(
            ENTRY_MEDIA_SESSION_RECORDING_CONSEQUENCES.none(execution::permits),
            "Incognito policy must suppress every recording consequence",
        )
    }
}
