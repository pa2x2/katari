package eu.kanade.tachiyomi.ui.reader.model

internal class InsertPage(val parent: ReaderPage) : ReaderPage(parent.index, parent.url, parent.imageUrl) {

    override var chapter: ReaderChapter = parent.chapter

    init {
        status = State.Ready
        stream = parent.stream
    }
}
