package app.katari.sync.ui.question

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.profiles.core.ProfileDatabase
import app.katari.sync.client.question.Question
import app.katari.sync.client.record.LocalRef
import app.katari.sync.protocol.device.DeviceId
import dev.zacsweers.metro.Inject
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.entry.EntryMapper
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.service.SourceManager

/**
 * Resolves what questions name (D029). Sync keeps entries by record key and profiles by uuid; entries are found
 * through the local row their record maps to, which a question's entries always have once its records were applied,
 * in whichever profile holds them, and profiles by uuid, a profile kept for now included. Other devices are
 * named from the account's device list.
 */
@Inject
class QuestionNames(
    private val handler: DatabaseHandler,
    private val sources: SourceManager,
    private val profiles: ProfileDatabase,
) {
    /** [questions] named for a device whose own ID is [thisDevice], while [activeProfileId] is in use. */
    suspend fun name(
        questions: List<Question>,
        devices: Map<DeviceId, String>,
        thisDevice: DeviceId?,
        activeProfileId: Long,
    ): List<NamedQuestion> {
        fun <T> Question.Made<T>.made() =
            Made(if (device == thisDevice) MadeOn.ThisDevice else MadeOn.Other(devices[device]), atMillis)

        suspend fun profileOf(entry: Entry?) = entry?.let { profiles.getProfileById(it.profileId) }
            ?.let { QuestionProfile(it.name, isActive = it.id == activeProfileId) }

        return questions.map { question ->
            when (question) {
                is Question.Migration -> {
                    val from = entry(question.from)
                    val targets = question.targets.map { target ->
                        val entry = entry(target.value)
                        NamedQuestion.Migration.Target(
                            key = target.value,
                            entry = entry,
                            source = entry?.let { sources.getDisplayInfo(it.source).name },
                            made = target.made(),
                        )
                    }
                    NamedQuestion.Migration(
                        question = question,
                        entryTitle = from?.title,
                        profile = profileOf(from ?: targets.firstNotNullOfOrNull { it.entry }),
                        targets = targets,
                    )
                }
                is Question.Notes -> {
                    val entry = entry(question.entryKey)
                    NamedQuestion.Notes(
                        question = question,
                        entryTitle = entry?.title,
                        profile = profileOf(entry),
                        versions = question.versions.map { NamedQuestion.Notes.Version(it.value, it.made()) },
                    )
                }
                is Question.ProfileDeletion -> NamedQuestion.ProfileDeletion(
                    question = question,
                    profileName = profiles.getProfileByUuid(question.profileUuid)?.name,
                    deletedOn = question.deleted.made().on,
                    changedOn = question.changed.made().on,
                )
            }
        }
    }

    /** The local entry [key]'s record maps to, which questions find by the entry's ID. */
    suspend fun entryId(key: String): Long? = handler.await {
        sync_recordsQueries.getByKey(key).awaitAsOneOrNull()?.local_ref?.let { LocalRef(it).long(0) }
    }

    private suspend fun entry(key: String): Entry? = entryId(key)?.let { id ->
        handler.awaitOneOrNull { entriesQueries.getEntryByIdAcrossProfiles(id, EntryMapper::mapEntry) }
    }
}
