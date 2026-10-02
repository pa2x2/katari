package mihon.feature.library.update.planning

import mihon.entry.interactions.state.EntryUpdateSkipReason
import tachiyomi.domain.library.update.model.LibraryUpdateSkipRules

/** One of the switches in [LibraryUpdateSkipRules], for screens that list them one by one. */
enum class LibraryUpdateSkipRule(
    val only: LibraryUpdateSkipRules,
    val reason: EntryUpdateSkipReason,
) {
    COMPLETED(LibraryUpdateSkipRules.None.copy(skipCompleted = true), EntryUpdateSkipReason.COMPLETED),
    UNSEEN(LibraryUpdateSkipRules.None.copy(skipUnseen = true), EntryUpdateSkipReason.NOT_CAUGHT_UP),
    NOT_STARTED(LibraryUpdateSkipRules.None.copy(skipNotStarted = true), EntryUpdateSkipReason.NOT_STARTED),
    OUTSIDE_RELEASE_PERIOD(
        LibraryUpdateSkipRules.None.copy(skipOutsideReleasePeriod = true),
        EntryUpdateSkipReason.OUTSIDE_RELEASE_PERIOD,
    ),
    ;

    fun isOn(rules: LibraryUpdateSkipRules): Boolean = when (this) {
        COMPLETED -> rules.skipCompleted
        UNSEEN -> rules.skipUnseen
        NOT_STARTED -> rules.skipNotStarted
        OUTSIDE_RELEASE_PERIOD -> rules.skipOutsideReleasePeriod
    }

    fun set(rules: LibraryUpdateSkipRules, on: Boolean): LibraryUpdateSkipRules = when (this) {
        COMPLETED -> rules.copy(skipCompleted = on)
        UNSEEN -> rules.copy(skipUnseen = on)
        NOT_STARTED -> rules.copy(skipNotStarted = on)
        OUTSIDE_RELEASE_PERIOD -> rules.copy(skipOutsideReleasePeriod = on)
    }
}
