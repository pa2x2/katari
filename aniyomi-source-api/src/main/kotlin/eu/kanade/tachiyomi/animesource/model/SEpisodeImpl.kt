@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.animesource.model

import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY

// Not in any extensions-lib; extensions built against Aniyomi's own classes construct it directly
class SEpisodeImpl : SEpisode {
    override lateinit var url: String
    override lateinit var name: String
    override var episode_number: Float = -1f
    override var fillermark: Boolean = false
    override var scanlator: String? = null
    override var date_upload: Long = 0
    override var summary: String? = null
    override var preview_url: String? = null
    override var memo: JsonObject = JsonObject.EMPTY
}
