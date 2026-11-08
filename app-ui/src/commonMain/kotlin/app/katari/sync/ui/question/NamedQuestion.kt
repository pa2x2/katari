package app.katari.sync.ui.question

import app.katari.sync.client.question.Question
import tachiyomi.domain.entry.model.Entry

/**
 * A [Question] with everything it names resolved for the person (D029): entries by title in whichever profile holds
 * them (D017), migration targets by their entry and source, profiles and devices by name. A name that can't be found
 * is null, and the screen words it generically.
 */
sealed interface NamedQuestion {
    val question: Question

    data class Migration(
        override val question: Question.Migration,
        val entryTitle: String?,
        val profile: QuestionProfile?,
        /** In the question's order, oldest first; the last is the one later edits follow until answered. */
        val targets: List<Target>,
    ) : NamedQuestion {
        /** One migration: the [entry] it went to, null once that is gone, on its [source]. */
        data class Target(val key: String, val entry: Entry?, val source: String?, val made: Made)
    }

    data class Notes(
        override val question: Question.Notes,
        val entryTitle: String?,
        val profile: QuestionProfile?,
        val versions: List<Version>,
    ) : NamedQuestion {
        data class Version(val notes: String, val made: Made)
    }

    data class ProfileDeletion(
        override val question: Question.ProfileDeletion,
        val profileName: String?,
        val deletedOn: MadeOn,
        val changedOn: MadeOn,
    ) : NamedQuestion
}

/** The profile a question's entry is in, and whether it is the one in use, where the entry can be opened. */
data class QuestionProfile(val name: String, val isActive: Boolean)

/** A change a question names, where and when (epoch milliseconds) it was made. */
data class Made(val on: MadeOn, val atMillis: Long)

/** The device a change was made on: this one (D005), or another, named while the account lists it. */
sealed interface MadeOn {
    data object ThisDevice : MadeOn

    data class Other(val name: String?) : MadeOn
}
