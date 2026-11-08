package app.katari.entry.interactions.media.handoff

/** A place in an entry: the chapter's index in reading order, and how far into the chapter. */
data class ReadingPlace(val chapterIndex: Int, val position: Long)

/**
 * Whether a reader at [here] offers to go to [there] (D053): when [there] is in a later chapter, or at least
 * [minimumStep] further on in the same one, so never for a page or two, nor for a place behind.
 */
fun isWorthOffering(here: ReadingPlace, there: ReadingPlace, minimumStep: Long): Boolean = when {
    there.chapterIndex != here.chapterIndex -> there.chapterIndex > here.chapterIndex
    else -> there.position - here.position >= minimumStep
}
