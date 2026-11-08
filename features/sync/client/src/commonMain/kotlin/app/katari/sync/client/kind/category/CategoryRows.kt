package app.katari.sync.client.kind.category

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.merge.RecordState
import app.katari.sync.client.payload.Tombstone
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordGroups
import app.katari.sync.client.record.newChangeId

/**
 * Writes a category record to its row, merging it with a category of the same name in the profile (D018).
 *
 * Two devices that each made a category called "Reading" made two records with random keys. Each device that meets
 * both keeps one row, owned by the record with the smaller key; the other record is deleted as replaced by it, so its
 * links and update rules apply to the kept row everywhere. Every device picks the same record whichever it met
 * first, and the devices that make the deletion make the same one, so it merges without a question.
 */
internal class CategoryRows(
    private val writes: LocalWrites,
    private val key: String,
    private val payload: CategoryPayload,
) {
    private val queries = writes.database.sync_applyQueries
    private val records = writes.database.sync_recordsQueries

    suspend fun apply(): ApplyOutcome {
        val profileId = writes.profileId(payload.profileUuid).idOr { return it }
        ownRow()?.let { return update(it) }

        val namesake = queries.categoriesNamed(profileId, payload.name.value).awaitAsList().firstOrNull()
            ?: return ApplyOutcome.Applied(
                CategoryKind.ref(
                    queries.insertCategory(profileId, payload.name.value, payload.order.value, payload.flags.value)
                        .awaitAsOne(),
                ),
            )
        val other = records.getByLocalRef(CategoryKind.name, CategoryKind.ref(namesake).json).awaitAsOneOrNull()
            ?.takeUnless { it.deleted }
        if (other != null && other.record_key < key) {
            return ApplyOutcome.Superseded(Tombstone(writes.newEdit(), replacedBy = other.record_key))
        }
        if (other != null) {
            val tombstone =
                RecordState.Deleted(Tombstone(writes.newEdit(), replacedBy = key, last = other.payload)).bytes
            records.storeLocalChange(
                recordKey = other.record_key,
                kind = CategoryKind.name,
                localRef = null,
                kindVersion = CategoryKind.version.toLong(),
                payload = tombstone,
                deleted = true,
                changeId = newChangeId(),
                recordGroup = RecordGroups.of(CategoryKind.name, other.record_key, deleted = true, tombstone),
            )
        }
        return update(namesake)
    }

    private suspend fun ownRow(): Long? {
        val ref = records.getByKey(key).awaitAsOneOrNull()?.local_ref ?: return null
        return LocalRef(ref).long(0).takeIf { queries.categoryExists(it).awaitAsOne() }
    }

    private suspend fun update(categoryId: Long): ApplyOutcome {
        queries.updateCategory(payload.name.value, payload.order.value, payload.flags.value, categoryId)
        return ApplyOutcome.Applied(CategoryKind.ref(categoryId))
    }
}
