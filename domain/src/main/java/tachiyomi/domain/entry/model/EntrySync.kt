package tachiyomi.domain.entry.model

import java.io.Serializable

data class EntrySync(
    val id: Long,
    val entryId: Long,
    val syncId: Long,
    val remoteId: Long,
    val libraryId: Long?,
    val title: String,
    val progress: Double,
    val total: Long,
    val status: Long,
    val score: Double,
    val remoteUrl: String,
    val startDate: Long,
    val finishDate: Long,
    val private: Boolean,
) : Serializable
