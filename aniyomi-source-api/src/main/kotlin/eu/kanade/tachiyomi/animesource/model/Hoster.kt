package eu.kanade.tachiyomi.animesource.model

import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY

/**
 * A server that offers an episode's videos. [videoList] is null when the videos have to be fetched with
 * `getVideoList(hoster)`; with [lazy] the host waits until the user picks this hoster to do that.
 */
open class Hoster(
    val hosterUrl: String = "",
    val hosterName: String = "",
    val videoList: List<Video>? = null,
    val internalData: String = "",
    val lazy: Boolean = false,
    val memo: JsonObject = JsonObject.EMPTY,
) {
    // The extensions-lib 16 constructor, before memo
    @Deprecated("Binary compatibility with extensions-lib 16 only", level = DeprecationLevel.HIDDEN)
    constructor(
        hosterUrl: String = "",
        hosterName: String = "",
        videoList: List<Video>? = null,
        internalData: String = "",
        lazy: Boolean = false,
    ) : this(hosterUrl, hosterName, videoList, internalData, lazy, JsonObject.EMPTY)

    fun copy(
        hosterUrl: String = this.hosterUrl,
        hosterName: String = this.hosterName,
        videoList: List<Video>? = this.videoList,
        internalData: String = this.internalData,
        lazy: Boolean = this.lazy,
        memo: JsonObject = this.memo,
    ): Hoster = Hoster(hosterUrl, hosterName, videoList, internalData, lazy, memo)

    companion object {
        /** The name of the single hoster that wraps the videos of a source without hosters. */
        const val NO_HOSTER_LIST = "no_hoster_list"

        fun List<Video>.toHosterList(): List<Hoster> {
            return listOf(
                Hoster(hosterUrl = "", hosterName = NO_HOSTER_LIST, videoList = this, memo = JsonObject.EMPTY),
            )
        }
    }
}
