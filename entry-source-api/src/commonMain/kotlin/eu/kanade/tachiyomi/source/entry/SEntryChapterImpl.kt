package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY

class SEntryChapterImpl : SEntryChapter {

    override lateinit var url: String

    override lateinit var name: String

    override var dateUpload: Long = 0

    override var chapterNumber: Double = (-1).toDouble()

    override var scanlator: String? = null

    override var memo: JsonObject = JsonObject.EMPTY
}
