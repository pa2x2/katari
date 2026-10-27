package tachiyomi.data.entry

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState

object EntryProgressStateMapper {
    fun mapState(
        @Suppress("UNUSED_PARAMETER")
        id: Long,
        entryId: Long,
        chapterId: Long?,
        contentKey: String,
        resourceKey: String,
        resourceRevision: String?,
        locatorKind: String,
        position: Long?,
        extent: Long?,
        progression: Double?,
        totalProgression: Double?,
        extensions: String,
        completed: Boolean,
        locatorUpdatedAt: Long,
        completionUpdatedAt: Long,
    ): EntryProgressState {
        return EntryProgressState(
            entryId = entryId,
            chapterId = chapterId,
            contentKey = contentKey,
            resourceKey = resourceKey,
            resourceRevision = resourceRevision,
            locator = EntryProgressLocator(
                kind = locatorKind,
                position = position,
                extent = extent,
                progression = progression,
                totalProgression = totalProgression,
                extensions = Json.parseToJsonElement(extensions).jsonObject,
            ),
            completed = completed,
            locatorUpdatedAt = locatorUpdatedAt,
            completionUpdatedAt = completionUpdatedAt,
        )
    }
}
