package eu.kanade.tachiyomi.source.entry

/**
 * Default mutable implementation returned by [SEntryChapterGroup.create].
 *
 * @suppress Use [SEntryChapterGroup.create] instead of depending on this implementation class.
 */
class SEntryChapterGroupImpl : SEntryChapterGroup {

    override lateinit var key: String

    override lateinit var title: String
}
