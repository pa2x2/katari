package app.katari.sync.client.question

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplySession
import app.katari.sync.client.apply.SyncWriting
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.kind.question.MigrationQuestionKind
import app.katari.sync.client.kind.question.NotesQuestionKind
import app.katari.sync.client.kind.question.ProfileDeletionAnswer
import app.katari.sync.client.kind.question.ProfileDeletionQuestionKind
import app.katari.sync.client.kind.question.QuestionKind
import app.katari.sync.client.kind.replacement.EntryReplacementKind
import app.katari.sync.client.merge.RecordState
import app.katari.sync.client.payload.Stamped
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.DatabaseHandler

/** A conflict sync leaves to the person (D018), the same on every device until answered on any. */
sealed interface Question {
    /**
     * The entry [from] migrated to different entries on two devices. Until answered, every device follows
     * [provisional], the later migration.
     */
    data class Migration(val from: String, val targets: List<String>, val provisional: String) : Question

    /** Notes on [entryKey] written on two devices at once; until answered the later [versions] are shown. */
    data class Notes(val entryKey: String, val versions: List<String>) : Question

    /** A profile deleted on one device while another device changed things in it; it stays until answered. */
    data class ProfileDeletion(val profileUuid: String) : Question
}

/**
 * The open questions, and the answers to them. Entries and profiles are named by their record keys and uuids, which
 * the screens resolve to what they show. An answer is a change of this device's like any edit, pushed with the next
 * sync, and settles the question on every device that receives it.
 */
@Inject
class Questions(
    private val handler: DatabaseHandler,
    private val writing: SyncWriting,
) {
    fun open(): Flow<List<Question>> = handler
        .subscribeToList { sync_recordsQueries.byKinds(kinds.map { it.name }) }
        .map { records -> records.mapNotNull { record -> question(record.kind, record.payload) } }

    /** Keeps the migration of [from] to [target], one of the question's targets. */
    suspend fun keepMigration(from: String, target: String) = writing.write { session ->
        val key = EntryReplacementKind.key(from)
        val stored = session.live(key)?.let(EntryReplacementKind::decode) ?: return@write
        val question = session.stored(MigrationQuestionKind, MigrationQuestionKind.key(from)) ?: return@write
        require(question.targets.value.any { it.to == target }) { "Not one of the question's targets" }
        if (stored.replacement.value.to != target) {
            val replacement = stored.replacement.value.copy(to = target)
            session.change(
                EntryReplacementKind,
                key,
                stored.copy(replacement = Stamped(replacement, session.newEdit())),
            )
        }
        val answer = question.copy(answer = Stamped(target, session.newEdit()))
        session.change(MigrationQuestionKind, MigrationQuestionKind.key(from), answer)
    }

    /** Keeps [notes] on [entryKey], one of the question's versions or the person's own merge of them. */
    suspend fun keepNotes(entryKey: String, notes: String) = writing.write { session ->
        val entry = session.live(entryKey)?.let(EntryKind::decode) ?: return@write
        val questionKey = NotesQuestionKind.key(entryKey)
        val question = session.stored(NotesQuestionKind, questionKey) ?: return@write
        session.change(EntryKind, entryKey, entry.copy(notes = Stamped(notes, session.newEdit())))
        session.change(NotesQuestionKind, questionKey, question.copy(answer = Stamped(notes, session.newEdit())))
    }

    suspend fun answerProfileDeletion(profileUuid: String, answer: ProfileDeletionAnswer) = writing.write { session ->
        val questionKey = ProfileDeletionQuestionKind.key(profileUuid)
        val question = session.stored(ProfileDeletionQuestionKind, questionKey) ?: return@write
        if (answer == ProfileDeletionAnswer.KEEP) {
            val key = ProfileKind.key(profileUuid)
            val profile = sync_recordsQueries.getByKey(key).awaitAsOneOrNull()?.let {
                when (val state = RecordState.of(it.deleted, it.payload)) {
                    is RecordState.Live -> state.bytes
                    is RecordState.Deleted -> state.tombstone.last.takeIf { last -> last.isNotEmpty() }
                }
            }
            profile?.let {
                session.change(ProfileKind, key, ProfileKind.kept(ProfileKind.decode(it), session.newEdit()))
            }
        }
        session.change(
            ProfileDeletionQuestionKind,
            questionKey,
            question.copy(answer = Stamped(answer, session.newEdit())),
        )
    }

    private suspend fun ApplySession.live(key: String): ByteArray? =
        database.sync_recordsQueries.getByKey(key).awaitAsOneOrNull()?.takeUnless { it.deleted }?.payload

    private suspend fun <P : Any> ApplySession.stored(kind: QuestionKind<P>, key: String): P? =
        live(key)?.let(kind::decode)

    private companion object {
        val kinds: List<QuestionKind<*>> = listOf(MigrationQuestionKind, NotesQuestionKind, ProfileDeletionQuestionKind)

        fun question(kind: String, payload: ByteArray): Question? = when (kind) {
            MigrationQuestionKind.name -> MigrationQuestionKind.decode(payload).takeIf(MigrationQuestionKind::isOpen)
                ?.let { question ->
                    val targets = question.targets.value.sortedBy { it.at }
                    Question.Migration(question.from, targets.map { it.to }, targets.last().to)
                }
            NotesQuestionKind.name -> NotesQuestionKind.decode(payload).takeIf(NotesQuestionKind::isOpen)
                ?.let { question -> Question.Notes(question.entryKey, question.versions.value.map { it.notes }) }
            ProfileDeletionQuestionKind.name -> ProfileDeletionQuestionKind.decode(payload)
                .takeIf(ProfileDeletionQuestionKind::isOpen)
                ?.let { question -> Question.ProfileDeletion(question.profileUuid) }
            else -> null
        }
    }
}
