package tachiyomi.domain.entry.model

import kotlinx.serialization.Serializable

@Serializable
enum class EntryStatus(val value: Int) {
    UNKNOWN(0),
    ONGOING(1),
    COMPLETED(2),
    LICENSED(3),
    PUBLISHING_FINISHED(4),
    CANCELLED(5),
    ON_HIATUS(6),
    ;

    companion object {
        fun from(value: Int?): EntryStatus {
            return entries.find { it.value == value } ?: UNKNOWN
        }
    }
}
