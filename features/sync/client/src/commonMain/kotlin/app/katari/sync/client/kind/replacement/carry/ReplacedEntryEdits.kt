package app.katari.sync.client.kind.replacement.carry

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.kind.category.CategoryMembershipKind
import app.katari.sync.client.kind.category.CategoryMembershipPayload
import app.katari.sync.client.kind.chapter.ChapterKind
import app.katari.sync.client.kind.chapter.ChapterPayload
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.entry.EntryPayload
import app.katari.sync.client.kind.entrychoice.DownloadPreferencesKind
import app.katari.sync.client.kind.entrychoice.EntryUpdateModeKind
import app.katari.sync.client.kind.entrychoice.ExcludedScanlatorKind
import app.katari.sync.client.kind.entrychoice.IntroTimesPickKind
import app.katari.sync.client.kind.entrychoice.PlaybackPreferencesKind
import app.katari.sync.client.kind.entrychoice.TheIntroDbPickKind
import app.katari.sync.client.kind.entrychoice.TranslationLanguagesKind
import app.katari.sync.client.kind.entrychoice.ViewerSettingKind
import app.katari.sync.client.kind.history.HistoryKind
import app.katari.sync.client.kind.history.HistoryPayload
import app.katari.sync.client.kind.progress.ProgressKind
import app.katari.sync.client.kind.progress.ProgressPayload
import app.katari.sync.client.kind.replacement.EntryReplacementKind
import app.katari.sync.client.kind.tracking.TrackerBindingKind
import app.katari.sync.client.record.RecordGroups
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.client.record.fillRecordGroups

/**
 * Carries edits made to an entry after it was replaced over to the entry that replaced it (D018): a device that
 * hadn't seen the replacement yet edits the old entry, and the edit lands on the new one on every device.
 *
 * Only edits stamped after the replacement carry over, and only what the replacement itself carried. A migration
 * carries what the app's own migration does, by its rules and the options the person chose ([CarriedLibraryState],
 * [CarriedChapterState], [CarriedEntryChoices]); its history and activity stay with the old entry, as they do when
 * migrating here. A season folding into the entry its merged entry migrated to carries only its chapter state, as the
 * new entry's place in the library and the choices about it are the merged entry's. A move to another profile is the
 * same entry in a new place, so everything follows it but its category links, which a local move drops too. Each device
 * carries the same edits to the same records, so the changes it pushes merge into one.
 */
internal class ReplacedEntryEdits(private val writes: LocalWrites) {

    /** Carries the record [key] of [kind], just applied, over when its entry was replaced. */
    suspend fun <P : Any> carry(kind: SyncedKind<P>, key: String, payload: P) {
        val from = if (kind === EntryKind) key else kind.entryKeyOf(payload) ?: return
        val carry = carryFor(from) ?: return
        carry(carry, key, payload)
    }

    /**
     * Whether [payload] belongs to an entry that moved to another profile, whose rows now belong to the records of the
     * entry it became: the record writes none of them, and what it changed after the move carries over instead.
     */
    suspend fun <P : Any> movedAway(kind: SyncedKind<P>, payload: P): Boolean {
        if (kind === EntryKind || (kind !== ChapterKind && kind !in CARRIED)) return false
        val entryKey = kind.entryKeyOf(payload) ?: return false
        return writes.movedTo(entryKey) != null
    }

    /**
     * Carries over every record of the entry [from] this device holds, once its replacement arrived: the records in
     * its group ([RecordGroups.entry]), the entry's own among them.
     */
    suspend fun carryAll(from: String) {
        val carry = carryFor(from) ?: return
        writes.database.fillRecordGroups()
        CarriedChapterState(carry).chapterMarks()
        CARRIED.forEach { kind ->
            writes.database.sync_recordsQueries.liveOfKindInGroup(RecordGroups.entry(from), kind.name).awaitAsList()
                .forEach { record -> carry(carry, record.record_key, kind.decode(record.payload)) }
        }
    }

    /** Carries [payload], the record [key], over the replacement [carry]. */
    private suspend fun carry(carry: ReplacementCarry, key: String, payload: Any) {
        when (payload) {
            // The marks are decided from all of the old entry's chapters, so the chapters arriving together, as
            // marking a whole entry read sends them, decide them once
            is ChapterPayload -> if (carry.after(payload.read) != null || carry.after(payload.bookmark) != null) {
                writes.afterRecords(CHAPTER_MARKS_OF + carry.from, cause = key) {
                    carryFor(carry.from)?.let { CarriedChapterState(it).chapterMarks() }
                }
            }
            is ProgressPayload -> CarriedChapterState(carry).progress(payload)
            is HistoryPayload -> CarriedChapterState(carry).history(payload)
            else -> if (!carry.chapterStateOnly) carryEntryState(carry, payload)
        }
    }

    /** Carries [payload], a record of the entry's place in the library or of the person's choices about it. */
    private suspend fun carryEntryState(carry: ReplacementCarry, payload: Any) {
        when (payload) {
            is EntryPayload -> CarriedLibraryState(carry).entry(payload)
            is CategoryMembershipPayload -> CarriedLibraryState(carry).membership(payload)
            else -> CarriedEntryChoices(carry).choice(payload)
        }
    }

    private suspend fun carryFor(from: String): ReplacementCarry? {
        val stored = writes.database.sync_recordsQueries.getByKey(EntryReplacementKind.key(from)).awaitAsOneOrNull()
        if (stored == null || stored.deleted) return null
        return ReplacementCarry(writes, EntryReplacementKind.decode(stored.payload))
    }

    private companion object {
        const val CHAPTER_MARKS_OF = "chapter-marks:"

        /**
         * Each kind whose records belong to an entry and may carry over, parents before what refers to them. Chapter
         * marks aren't among them: they are carried for the whole entry at once, since migrating decides each from all
         * of them.
         */
        val CARRIED: List<SyncedKind<*>> = listOf(
            EntryKind,
            ProgressKind,
            HistoryKind,
            CategoryMembershipKind,
            TrackerBindingKind,
            ViewerSettingKind,
            PlaybackPreferencesKind,
            TranslationLanguagesKind,
            EntryUpdateModeKind,
            ExcludedScanlatorKind,
            DownloadPreferencesKind,
            IntroTimesPickKind,
            TheIntroDbPickKind,
        )
    }
}
