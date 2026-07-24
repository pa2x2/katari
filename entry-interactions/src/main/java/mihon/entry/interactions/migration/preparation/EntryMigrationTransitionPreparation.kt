package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionFailurePolicy
import mihon.feature.graph.FeatureExecutionPointId
import mihon.feature.graph.FeatureExecutionRuntime
import mihon.feature.graph.inlineFeatureExecutionPointDefinition
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.track.model.EntryTrack

internal data class EntryMigrationTransitionPreparingEvent(
    val source: Entry,
    val target: Entry,
    val sourceTracks: List<EntryTrack>,
    val outcomes: EntryMigrationTransitionPreparationSink,
)

internal fun interface EntryMigrationTransitionPreparationSink {
    fun addTracks(tracks: List<EntryTrack>)
}

internal val ENTRY_MIGRATION_TRANSITION_PREPARING_POINT =
    inlineFeatureExecutionPointDefinition<EntryMigrationTransitionPreparingEvent>(
        id = FeatureExecutionPointId("entry.migration.transition-preparing"),
        owner = ENTRY_MIGRATION_FEATURE_OWNER,
        failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
    )

internal sealed interface EntryMigrationTransitionPreparationResult {
    data class Prepared(val tracks: List<EntryTrack>) : EntryMigrationTransitionPreparationResult
    data object Failed : EntryMigrationTransitionPreparationResult
}

internal class EntryMigrationTransitionPreparation(
    private val executions: FeatureExecutionRuntime,
) {
    suspend fun prepare(
        source: Entry,
        target: Entry,
        sourceTracks: List<EntryTrack>,
    ): EntryMigrationTransitionPreparationResult {
        val preparedTracks = mutableListOf<EntryTrack>()
        val result = executions.executeInline(
            point = ENTRY_MIGRATION_TRANSITION_PREPARING_POINT,
            contentType = source.type.toContentTypeId(),
            event = EntryMigrationTransitionPreparingEvent(source, target, sourceTracks, preparedTracks::addAll),
        )
        return if (result.isSuccessful) {
            EntryMigrationTransitionPreparationResult.Prepared(preparedTracks)
        } else {
            EntryMigrationTransitionPreparationResult.Failed
        }
    }
}
