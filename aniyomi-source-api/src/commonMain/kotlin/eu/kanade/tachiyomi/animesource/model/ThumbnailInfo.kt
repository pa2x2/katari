package eu.kanade.tachiyomi.animesource.model

open class ThumbnailInfo(
    val tileInfo: List<TileInfo>,
    val imageTileUrls: List<String>,
)

/** One seek-bar thumbnail: where it sits in image tile [imageIndex], and the time it shows. */
data class TileInfo(
    val imageIndex: Int,
    val timeMs: Long,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)
