package mihon.entry.interactions

import android.app.PendingIntent
import android.content.Context
import io.mockk.mockk
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryOpenContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryOpenFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(ENTRY_OPEN_FEATURE_ID, EntryOpenBehaviorContract),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryOpenCapability.definition)
                    val evaluation = productionSubjectEvaluation(
                        EntryOpenCapability.bind(provider),
                        EntryOpenFeatureContributor,
                    )
                    val context = mockk<Context>(relaxed = true)
                    val pendingIntent = mockk<PendingIntent>(relaxed = true)
                    val entry = Entry.create().copy(id = 41L, type = provider.type)
                    val chapter = EntryChapter.create().copy(id = 73L)
                    val opened = mutableListOf<Pair<Long, Long>>()
                    val feature = DefaultEntryOpenFeature(
                        evaluation = evaluation,
                        interaction = object : EntryOpenInteraction {
                            override fun open(
                                context: Context,
                                entry: Entry,
                                chapter: EntryChapter,
                                options: EntryOpenOptions,
                            ) {
                                opened += entry.id to chapter.id
                            }

                            override fun pendingIntent(
                                context: Context,
                                entry: Entry,
                                chapter: EntryChapter,
                                options: EntryOpenOptions,
                            ): PendingIntent = pendingIntent
                        },
                    )

                    contractExpectation(feature.isApplicable(provider.type), "Open must be applicable")
                    contractExpectation(feature.open(context, entry, chapter), "Open must dispatch")
                    contractExpectation(opened == listOf(entry.id to chapter.id), "Open dispatched the wrong subject")
                    contractExpectation(
                        feature.pendingIntent(context, entry, chapter) === pendingIntent,
                        "Open must expose its action",
                    )
                }
            },
        )
    }
}

class EntryContinueContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryContinueFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(ENTRY_CONTINUE_FEATURE_ID, EntryContinueBehaviorContract),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryContinueCapability.definition)
                    val evaluation = productionSubjectEvaluation(
                        EntryContinueCapability.bind(provider),
                        EntryContinueFeatureContributor,
                    )
                    val context = mockk<Context>(relaxed = true)
                    val entry = Entry.create().copy(id = 42L, type = provider.type)
                    val next = EntryChapter.create().copy(id = 74L)
                    val opened = mutableListOf<Long>()
                    val feature = DefaultEntryContinueFeature(
                        evaluation = evaluation,
                        interaction = object : EntryContinueInteraction {
                            override suspend fun continueEntry(context: Context, entry: Entry): EntryChapter {
                                opened += entry.id
                                return next
                            }

                            override suspend fun findNext(entry: Entry): EntryChapter = next
                        },
                    )

                    contractExpectation(feature.isApplicable(provider.type), "Continue must be applicable")
                    contractExpectation(
                        feature.nextTarget(entry) == EntryContinueTargetResult.Available(next),
                        "Continue must expose the next target",
                    )
                    contractExpectation(
                        feature.continueEntry(context, entry) == EntryContinueResult.Opened(next),
                        "Continue must dispatch the next target",
                    )
                    contractExpectation(opened == listOf(entry.id), "Continue dispatched the wrong subject")
                }
            },
        )
    }
}
