package tachiyomi.data.updates

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.updates.model.UpdatesFeedRow
import tachiyomi.domain.updates.model.UpdatesWithRelations

object UpdatesMapper {
    @Suppress("UNUSED_PARAMETER")
    fun mapUpdatesWithRelations(
        profileId: Long,
        entryId: Long,
        entryType: String,
        entryTitle: String,
        chapterId: Long,
        chapterName: String,
        scanlator: String?,
        chapterUrl: String,
        read: Boolean,
        bookmark: Boolean,
        started: Long,
        progressPosition: Long,
        source: Long,
        favorite: Boolean,
        thumbnailUrl: String?,
        coverLastModified: Long,
        dateUpload: Long,
        dateFetch: Long,
        excludedScanlator: String?,
    ): UpdatesWithRelations = UpdatesWithRelations(
        entryId = entryId,
        entryType = EntryType.valueOf(entryType.uppercase()),
        entryTitle = entryTitle,
        chapterId = chapterId,
        chapterName = chapterName,
        scanlator = scanlator,
        chapterUrl = chapterUrl,
        read = read,
        bookmark = bookmark,
        started = started != 0L,
        progressPosition = progressPosition,
        sourceId = source,
        dateFetch = dateFetch,
        coverData = EntryCover(
            entryId = entryId,
            sourceId = source,
            isFavorite = favorite,
            url = thumbnailUrl,
            lastModified = coverLastModified,
        ),
        dateUpload = dateUpload,
    )

    fun mapFeedRow(
        profileId: Long,
        entryId: Long,
        entryType: String,
        entryTitle: String,
        chapterId: Long,
        chapterName: String,
        scanlator: String?,
        chapterUrl: String,
        read: Boolean,
        bookmark: Boolean,
        started: Long,
        progressPosition: Long,
        source: Long,
        favorite: Boolean,
        thumbnailUrl: String?,
        coverLastModified: Long,
        dateUpload: Long,
        dateFetch: Long,
        excludedScanlator: String?,
        categoryIds: String,
    ): UpdatesFeedRow = UpdatesFeedRow(
        update = mapUpdatesWithRelations(
            profileId, entryId, entryType, entryTitle, chapterId, chapterName, scanlator, chapterUrl, read, bookmark,
            started, progressPosition, source, favorite, thumbnailUrl, coverLastModified, dateUpload, dateFetch,
            excludedScanlator,
        ),
        categoryIds = categoryIds
            .takeIf { it.isNotEmpty() }
            ?.split(',')
            ?.mapTo(mutableSetOf()) { it.toLong() }
            ?: setOf(Category.UNCATEGORIZED_ID),
    )
}
