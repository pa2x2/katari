package tachiyomi.domain.entry.model

import kotlinx.serialization.Serializable

/**
 * The cell of a three by three grid over the picture where a title's subtitles rest before the viewer drags them.
 * [key] is how it is stored, synced and backed up.
 */
@Serializable
enum class SubtitlePosition(val row: Row, val column: Column, val key: String) {
    TOP_LEFT(Row.TOP, Column.LEFT, "top_left"),
    TOP_CENTER(Row.TOP, Column.CENTER, "top_center"),
    TOP_RIGHT(Row.TOP, Column.RIGHT, "top_right"),
    MIDDLE_LEFT(Row.MIDDLE, Column.LEFT, "middle_left"),
    MIDDLE_CENTER(Row.MIDDLE, Column.CENTER, "middle_center"),
    MIDDLE_RIGHT(Row.MIDDLE, Column.RIGHT, "middle_right"),
    BOTTOM_LEFT(Row.BOTTOM, Column.LEFT, "bottom_left"),
    BOTTOM_CENTER(Row.BOTTOM, Column.CENTER, "bottom_center"),
    BOTTOM_RIGHT(Row.BOTTOM, Column.RIGHT, "bottom_right"),
    ;

    enum class Row { TOP, MIDDLE, BOTTOM }

    enum class Column { LEFT, CENTER, RIGHT }

    companion object {
        fun of(row: Row, column: Column): SubtitlePosition = entries.first { it.row == row && it.column == column }

        /** Null for a key this version doesn't know, which rests the subtitles at the bottom centre. */
        fun fromKey(key: String): SubtitlePosition? = entries.firstOrNull { it.key == key }
    }
}
