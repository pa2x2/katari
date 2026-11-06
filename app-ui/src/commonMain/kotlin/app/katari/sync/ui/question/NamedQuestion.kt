package app.katari.sync.ui.question

import app.katari.sync.client.question.Question

/**
 * A [Question] with everything it names resolved for the person (D029): entries by title, migration targets by their
 * source, profiles and devices by name. A name that can't be found is null, and the screen words it generically.
 */
sealed interface NamedQuestion {
    val question: Question

    data class Migration(
        override val question: Question.Migration,
        val entryTitle: String?,
        /** In the question's order, oldest first; the last is the one followed for now. */
        val targets: List<Target>,
    ) : NamedQuestion {
        data class Target(val key: String, val source: String?, val device: String?)

        val provisional: Target? get() = targets.firstOrNull { it.key == question.provisional }
    }

    data class Notes(
        override val question: Question.Notes,
        val entryTitle: String?,
        val versions: List<Version>,
    ) : NamedQuestion {
        data class Version(val notes: String, val device: String?, val atMillis: Long)
    }

    data class ProfileDeletion(
        override val question: Question.ProfileDeletion,
        val profileName: String?,
        val deletedOn: String?,
        val changedOn: String?,
    ) : NamedQuestion
}
