package app.katari.sync.ui.question

import app.katari.sync.client.question.Question

/** What waits for the person about one entry, which its screen shows as a banner (D029). */
enum class EntryQuestion {
    MIGRATION,
    NOTES,
    ;

    companion object {
        /**
         * The question about the entry [entryId] among [open], whose entry keys [entryIdOf] maps to local entries; a
         * migration counts for the entry it came from and every entry it went to. Migration wins, settling which
         * entry the notes belong to.
         */
        suspend fun of(open: List<Question>, entryId: Long, entryIdOf: suspend (String) -> Long?): EntryQuestion? {
            val migration = open.filterIsInstance<Question.Migration>().any { question ->
                (listOf(question.from) + question.targets.map { it.value }).any { entryIdOf(it) == entryId }
            }
            if (migration) return MIGRATION
            val notes = open.filterIsInstance<Question.Notes>().any { entryIdOf(it.entryKey) == entryId }
            return if (notes) NOTES else null
        }
    }
}
