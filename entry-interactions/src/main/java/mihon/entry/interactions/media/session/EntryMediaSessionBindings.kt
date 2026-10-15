package mihon.entry.interactions.media.session

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
import tachiyomi.domain.entry.repository.EntryRepository

@ContributesTo(AppScope::class)
@BindingContainer
object EntryMediaSessionBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun eventSink(feature: () -> EntryMediaSessionFeature): EntryMediaSessionEventSink =
        EntryMediaSessionEventSink { event ->
            feature().onEvent(event)
        }

    @Provides
    @SingleIn(AppScope::class)
    fun feature(composition: FeatureRuntimeComposition): EntryMediaSessionFeature = DefaultEntryMediaSessionFeature(
        evaluation = composition.evaluation,
        executions = composition.executions,
    )

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryMediaSessionFeatureContributor

    @Provides
    @IntoSet
    fun incognitoGraphContributor(): FeatureGraphContributor = EntryMediaSessionIncognitoContributor

    @Provides
    @IntoSet
    fun incognitoParticipant(
        repository: () -> EntryRepository,
        incognitoState: EntryMediaSessionIncognitoState,
    ): FeatureExecutionParticipantBinding<*> = entryMediaSessionIncognitoBinding(
        repository = repository,
        incognitoState = incognitoState,
    )

    @Provides
    @IntoSet
    fun exportEventSink(sink: () -> EntryMediaSessionEventSink): InjektExport<*> = injektExport(sink)

    @Provides
    @IntoSet
    fun exportFeature(feature: () -> EntryMediaSessionFeature): InjektExport<*> = injektExport(feature)
}

internal fun entryMediaSessionIncognitoBinding(
    repository: () -> EntryRepository,
    incognitoState: EntryMediaSessionIncognitoState,
) = FeatureExecutionParticipantBinding(
    definition = ENTRY_MEDIA_SESSION_INCOGNITO_PARTICIPANT,
    handler = FeatureExecutionHandler { execution ->
        val event = execution.event
        val owner = repository().getEntryById(event.child.entryId)
        val sourceId = owner?.source ?: event.visibleEntry.source
        if (incognitoState.isIncognito(sourceId)) {
            execution.block(ENTRY_MEDIA_SESSION_RECORDING_CONSEQUENCES)
        }
    },
)
