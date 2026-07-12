package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY

class SEntryImpl : SEntry {

    override lateinit var url: String

    override lateinit var title: String

    override var artist: String? = null

    override var author: String? = null

    override var description: String? = null

    override var genre: List<String>? = null

    override var status: Int = 0

    override var thumbnailUrl: String? = null

    override var updateStrategy: EntryUpdateStrategy? = null

    override var initialized: Boolean = false

    override var memo: JsonObject = JsonObject.EMPTY

    override var type: EntryType = EntryType.MANGA
}
