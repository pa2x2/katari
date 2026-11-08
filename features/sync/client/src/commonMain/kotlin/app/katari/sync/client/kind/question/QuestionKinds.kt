package app.katari.sync.client.kind.question

import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.kind.profile.restoreDeletedProfile
import app.katari.sync.client.merge.latest
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.SyncedKind
import kotlinx.serialization.KSerializer

/**
 * A kind of question, keyed by what it asks about, so the devices that notice the same conflict raise the same record.
 * Questions are made by sync itself, never captured from rows, and live in their records alone.
 */
sealed class QuestionKind<P : Any>(name: String, serializer: KSerializer<P>) :
    SyncedKind<P>(name, 1, serializer, Absence.KEEP) {
    override val capture: List<CaptureTrigger> = emptyList()

    fun key(subject: String): String = RecordKeys.derive(name, subject)

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<P> = LocalRead.NotSynced

    override suspend fun apply(writes: LocalWrites, key: String, payload: P): ApplyOutcome = ApplyOutcome.Applied(null)

    /** Whether [payload] still waits for an answer. */
    abstract fun isOpen(payload: P): Boolean
}

/** The same entry migrated to different entries on two devices. Subject: the migrated entry's key. */
object MigrationQuestionKind : QuestionKind<MigrationQuestionPayload>(
    "question.migration",
    MigrationQuestionPayload.serializer(),
) {
    override fun merge(local: MigrationQuestionPayload, incoming: MigrationQuestionPayload) = local.copy(
        targets = latest(local.targets, incoming.targets),
        answer = latest(local.answer, incoming.answer),
    )

    override fun isOpen(payload: MigrationQuestionPayload) = payload.answer.at < payload.targets.at
}

/** Notes on an entry edited on two devices at the same time. Subject: the entry's key. */
object NotesQuestionKind : QuestionKind<NotesQuestionPayload>("question.notes", NotesQuestionPayload.serializer()) {
    override fun merge(local: NotesQuestionPayload, incoming: NotesQuestionPayload) = local.copy(
        versions = latest(local.versions, incoming.versions),
        answer = latest(local.answer, incoming.answer),
    )

    override fun isOpen(payload: NotesQuestionPayload) = payload.answer.at < payload.versions.at

    override fun entryKeyOf(payload: NotesQuestionPayload) = payload.entryKey
}

/**
 * A profile deleted on one device while another changed things in it since. Subject: the profile's uuid. While it is
 * open the profile stays on every device, so a device that deleted it before hearing of the changes brings it back.
 * Keeping it is the profile's record edited after the deletion; deleting it applies the deletion again, now that
 * nothing holds it back.
 */
object ProfileDeletionQuestionKind : QuestionKind<ProfileDeletionQuestionPayload>(
    "question.profile-deletion",
    ProfileDeletionQuestionPayload.serializer(),
) {
    override fun merge(local: ProfileDeletionQuestionPayload, incoming: ProfileDeletionQuestionPayload) = local.copy(
        conflict = latest(local.conflict, incoming.conflict),
        answer = latest(local.answer, incoming.answer),
    )

    override fun isOpen(payload: ProfileDeletionQuestionPayload) = payload.answer.at < payload.conflict.at

    override suspend fun apply(
        writes: LocalWrites,
        key: String,
        payload: ProfileDeletionQuestionPayload,
    ): ApplyOutcome {
        when {
            isOpen(payload) -> restoreDeletedProfile(writes, payload.profileUuid)
            payload.answer.value == ProfileDeletionAnswer.DELETE ->
                writes.reapply(ProfileKind.key(payload.profileUuid))
            else -> Unit
        }
        return ApplyOutcome.Applied(null)
    }
}
