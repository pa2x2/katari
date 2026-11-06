package app.katari.sync.ui.question

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.question.Question
import app.katari.sync.client.record.LocalRef
import app.katari.sync.protocol.device.DeviceId
import dev.zacsweers.metro.Inject
import mihon.feature.profiles.core.ProfileDatabase
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager

/**
 * Resolves what questions name (D029). Sync keeps entries by record key and profiles by uuid; entries are found
 * through the local row their record maps to, which a question's entries always have once its records were applied,
 * and profiles by uuid, a profile kept for now included. Devices are named from the account's device list.
 */
@Inject
class QuestionNames(
    private val handler: DatabaseHandler,
    private val entries: EntryRepository,
    private val sources: SourceManager,
    private val profiles: ProfileDatabase,
) {
    suspend fun name(questions: List<Question>, devices: Map<DeviceId, String>): List<NamedQuestion> =
        questions.map { question ->
            when (question) {
                is Question.Migration -> NamedQuestion.Migration(
                    question = question,
                    entryTitle = entry(question.from)?.title,
                    targets = question.targets.map { target ->
                        NamedQuestion.Migration.Target(
                            key = target.value,
                            source = entry(target.value)?.let { sources.getDisplayInfo(it.source).name },
                            device = devices[target.device],
                        )
                    },
                )
                is Question.Notes -> NamedQuestion.Notes(
                    question = question,
                    entryTitle = entry(question.entryKey)?.title,
                    versions = question.versions.map {
                        NamedQuestion.Notes.Version(it.value, devices[it.device], it.atMillis)
                    },
                )
                is Question.ProfileDeletion -> NamedQuestion.ProfileDeletion(
                    question = question,
                    profileName = profiles.getProfileByUuid(question.profileUuid)?.name,
                    deletedOn = devices[question.deleted.device],
                    changedOn = devices[question.changed.device],
                )
            }
        }

    /** The local entry [key]'s record maps to, which questions find by the entry's ID. */
    suspend fun entryId(key: String): Long? = handler.await {
        sync_recordsQueries.getByKey(key).awaitAsOneOrNull()?.local_ref?.let { LocalRef(it).long(0) }
    }

    private suspend fun entry(key: String) = entryId(key)?.let { entries.getEntryById(it) }
}
