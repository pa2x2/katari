package mihon.entry.interactions.book.preparation

/**
 * How far opening a book has got. A large book can take seconds at several of these steps, so the reader names the
 * step it waits on and, when the step can tell, how much of it is done.
 */
internal sealed interface BookOpeningProgress {
    /** The source is asked for the chapter's book. */
    data object Requesting : BookOpeningProgress

    /** The book file is fetched; [fraction] is 0..1, or null while the source gives no size. */
    data class Downloading(val fraction: Float?) : BookOpeningProgress

    /** The copy prepared on an earlier open is read back; [fraction] is 0..1. */
    data class Restoring(val fraction: Float) : BookOpeningProgress

    /** The book is converted for the reader; [fraction] is 0..1. */
    data class Preparing(val fraction: Float) : BookOpeningProgress

    /** The prepared book is kept so later opens restore it instead. */
    data object Saving : BookOpeningProgress
}
