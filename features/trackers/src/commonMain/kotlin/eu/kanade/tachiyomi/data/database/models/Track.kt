@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface Track : Serializable {

    var id: Long?

    var manga_id: Long

    var tracker_id: Long

    var remote_id: Long

    var library_id: Long?

    var title: String

    var last_chapter_read: Double

    var total_chapters: Long

    var progress: Double
        get() = last_chapter_read
        set(value) {
            last_chapter_read = value
        }

    var total: Long
        get() = total_chapters
        set(value) {
            total_chapters = value
        }

    var score: Double

    var status: Long

    var started_reading_date: Long

    var finished_reading_date: Long

    var tracking_url: String

    var private: Boolean

    fun copyPersonalFrom(other: Track, copyRemotePrivate: Boolean = true) {
        progress = other.progress
        score = other.score
        status = other.status
        started_reading_date = other.started_reading_date
        finished_reading_date = other.finished_reading_date
        if (copyRemotePrivate) private = other.private
    }

    companion object {
        fun create(serviceId: Long): Track = TrackImpl().apply {
            tracker_id = serviceId
        }
    }
}
