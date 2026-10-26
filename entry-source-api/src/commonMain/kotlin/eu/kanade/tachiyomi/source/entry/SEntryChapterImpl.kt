package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY

/**
 * Default mutable implementation returned by [SEntryChapter.create].
 *
 * @suppress Use [SEntryChapter.create] instead of depending on this implementation class.
 */
class SEntryChapterImpl : SEntryChapter {

    override lateinit var url: String

    override lateinit var name: String

    override var dateUpload: Long = 0

    override var chapterNumber: Double = (-1).toDouble()

    override var scanlator: String? = null

    override var summary: String? = null

    override var previewUrl: String? = null

    override var kind: ChapterKind = ChapterKind.REGULAR

    override var group: String? = null

    override var dateText: String? = null

    override var memo: JsonObject = JsonObject.EMPTY
}
