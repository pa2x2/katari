package tachiyomi.domain.library.update.model

data class LibraryUpdateSkipRules(
    val skipCompleted: Boolean,
    val skipUnseen: Boolean,
    val skipNotStarted: Boolean,
    /** Skips entries whose predicted next release falls after the current fetch window. */
    val skipOutsideReleasePeriod: Boolean,
) {
    /** Skips when either rule set would skip, which is how an entry in several categories combines their rules. */
    infix fun strictest(other: LibraryUpdateSkipRules) = LibraryUpdateSkipRules(
        skipCompleted = skipCompleted || other.skipCompleted,
        skipUnseen = skipUnseen || other.skipUnseen,
        skipNotStarted = skipNotStarted || other.skipNotStarted,
        skipOutsideReleasePeriod = skipOutsideReleasePeriod || other.skipOutsideReleasePeriod,
    )

    companion object {
        val None = LibraryUpdateSkipRules(
            skipCompleted = false,
            skipUnseen = false,
            skipNotStarted = false,
            skipOutsideReleasePeriod = false,
        )
    }
}
