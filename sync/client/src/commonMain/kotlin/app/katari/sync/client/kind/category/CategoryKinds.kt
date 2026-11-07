package app.katari.sync.client.kind.category

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.capture.CaptureTrigger.Owner
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.merge.latest
import app.katari.sync.client.record.FieldStamps
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind

/**
 * Categories a person made, with random keys ([LocalReads.categoryKey]). Local ref: `[categories._id]`. The Default
 * category is never a record of its own.
 */
object CategoryKind : SyncedKind<CategoryPayload>(
    LocalReads.CATEGORY,
    1,
    CategoryPayload.serializer(),
    Absence.TOMBSTONE,
) {
    override val capture = listOf(
        CaptureTrigger("categories", Event.INSERT, listOf("NEW._id"), condition = "NEW._id != 0"),
        CaptureTrigger(
            "categories",
            Event.UPDATE,
            listOf("NEW._id"),
            fields = listOf(Field(2, "name"), Field(3, "sort"), Field(4, "flags")),
            condition = "NEW._id != 0",
        ),
        CaptureTrigger("categories", Event.DELETE, listOf("OLD._id"), condition = "OLD._id != 0"),
    )

    fun ref(categoryId: Long): LocalRef = LocalRef.of(categoryId)

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<CategoryPayload> {
        val row = reads.database.sync_local_rowsQueries.categoryRow(ref.long(0)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val profileUuid = reads.profileUuid(row.profile_id) ?: return LocalRead.Missing
        return LocalRead.Found(
            key = reads.categoryKey(row._id, row.profile_id) ?: return LocalRead.Missing,
            parents = listOf(RecordRef(ProfileKind, ProfileKind.ref(profileUuid))),
        ) { previous, stamps ->
            CategoryPayload(
                profileUuid = profileUuid,
                name = stamps.field(2, row.name, previous?.name),
                order = stamps.field(3, row.sort, previous?.order),
                flags = stamps.field(4, row.flags, previous?.flags),
            )
        }
    }

    override fun merge(local: CategoryPayload, incoming: CategoryPayload) = local.copy(
        name = latest(local.name, incoming.name),
        order = latest(local.order, incoming.order),
        flags = latest(local.flags, incoming.flags),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: CategoryPayload): ApplyOutcome =
        CategoryRows(writes, key, payload).apply()

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        // Its links go with it, so the entries only in it fall back to the Default category, as deleting it here does
        ref?.let { writes.database.sync_applyQueries.deleteCategory(it.long(0)) }
        return ApplyOutcome.Applied(null)
    }
}

/** Entry–category links, keyed by entry key and category key. Local ref: `[entry_id, category_id]`. */
object CategoryMembershipKind : SyncedKind<CategoryMembershipPayload>(
    "category.membership",
    1,
    CategoryMembershipPayload.serializer(),
    Absence.REMOVED_FLAG,
) {
    override val capture = listOf(
        CaptureTrigger("entries_categories", Event.INSERT, listOf("NEW.entry_id", "NEW.category_id")),
        CaptureTrigger(
            "entries_categories",
            Event.DELETE,
            listOf("OLD.entry_id", "OLD.category_id"),
            owners = listOf(Owner.entry()),
        ),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<CategoryMembershipPayload> {
        val entryId = ref.long(0)
        val categoryId = ref.long(1)
        reads.database.sync_local_rowsQueries.categoryMembershipRow(entryId, categoryId).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val entry = reads.entry(entryId) ?: return LocalRead.Missing
        val entryKey = reads.entryKeyOf(entry) ?: return LocalRead.NotSynced
        val categoryKey = reads.categoryKey(categoryId, entry.profile_id) ?: return LocalRead.Missing
        return LocalRead.Found(
            key = RecordKeys.derive(name, entryKey, categoryKey),
            parents = listOf(
                RecordRef(EntryKind, EntryKind.ref(entryId)),
                RecordRef(CategoryKind, CategoryKind.ref(categoryId)),
            ),
        ) { previous, stamps ->
            CategoryMembershipPayload(entryKey, categoryKey, stamps.field(3, false, previous?.removed))
        }
    }

    override fun removed(previous: CategoryMembershipPayload, stamps: FieldStamps) =
        previous.copy(removed = stamps.field(3, true, previous.removed))

    override fun merge(local: CategoryMembershipPayload, incoming: CategoryMembershipPayload) =
        local.copy(removed = latest(local.removed, incoming.removed))

    /** A link to a deleted category links nothing, which leaves the entry in the Default category if in no other. */
    override suspend fun apply(writes: LocalWrites, key: String, payload: CategoryMembershipPayload): ApplyOutcome {
        val entryId = writes.entryId(payload.entryKey).idOr { return it }
        val queries = writes.database.sync_applyQueries
        val profileId = queries.entryProfile(entryId).awaitAsOne()
        val profileUuid = writes.database.sync_local_rowsQueries.profileUuid(profileId).awaitAsOne()
        val categoryId = writes.categoryId(payload.categoryKey, profileUuid).idOr { return it }
        val linked = queries.membershipExists(entryId, categoryId).awaitAsOne()
        when {
            payload.removed.value -> queries.deleteMembership(entryId, categoryId)
            !linked -> queries.insertMembership(profileId, entryId, categoryId)
        }
        return ApplyOutcome.Applied(LocalRef.of(entryId, categoryId))
    }

    override fun entryKeyOf(payload: CategoryMembershipPayload) = payload.entryKey
}

/**
 * Per-category update rules, keyed by category key; the Default category's per profile. Local ref:
 * `[profile_id, category_id]`.
 */
object CategoryUpdateRulesKind : SyncedKind<CategoryUpdateRulesPayload>(
    "category.update-rules",
    1,
    CategoryUpdateRulesPayload.serializer(),
    Absence.TOMBSTONE,
) {
    private val newRef = listOf("NEW.profile_id", "NEW.category_id")

    override val capture = listOf(
        CaptureTrigger("category_update_rules", Event.INSERT, newRef),
        CaptureTrigger(
            "category_update_rules",
            Event.UPDATE,
            newRef,
            fields = listOf(
                Field(3, "auto_update"),
                Field(4, "custom_rules"),
                Field(5, "skip_completed"),
                Field(6, "skip_unseen"),
                Field(7, "skip_not_started"),
                Field(8, "skip_outside_release_period"),
                Field(9, "interval_hours"),
            ),
        ),
        CaptureTrigger("category_update_rules", Event.DELETE, listOf("OLD.profile_id", "OLD.category_id")),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<CategoryUpdateRulesPayload> {
        val row = reads.database.sync_local_rowsQueries.categoryUpdateRulesRow(ref.long(0), ref.long(1))
            .awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val profileUuid = reads.profileUuid(row.profile_id) ?: return LocalRead.Missing
        val categoryKey = reads.categoryKey(row.category_id, row.profile_id) ?: return LocalRead.Missing
        val parents = listOfNotNull(
            RecordRef(ProfileKind, ProfileKind.ref(profileUuid)),
            RecordRef(CategoryKind, CategoryKind.ref(row.category_id)).takeIf { row.category_id != 0L },
        )
        return LocalRead.Found(RecordKeys.derive(name, categoryKey), parents) { previous, stamps ->
            CategoryUpdateRulesPayload(
                profileUuid = profileUuid,
                categoryKey = categoryKey,
                autoUpdate = stamps.field(3, row.auto_update, previous?.autoUpdate),
                customRules = stamps.field(4, row.custom_rules, previous?.customRules),
                skipCompleted = stamps.field(5, row.skip_completed, previous?.skipCompleted),
                skipUnseen = stamps.field(6, row.skip_unseen, previous?.skipUnseen),
                skipNotStarted = stamps.field(7, row.skip_not_started, previous?.skipNotStarted),
                skipOutsideReleasePeriod = stamps.field(
                    8,
                    row.skip_outside_release_period,
                    previous?.skipOutsideReleasePeriod,
                ),
                intervalHours = stamps.field(9, row.interval_hours, previous?.intervalHours),
            )
        }
    }

    override fun merge(local: CategoryUpdateRulesPayload, incoming: CategoryUpdateRulesPayload) = local.copy(
        autoUpdate = latest(local.autoUpdate, incoming.autoUpdate),
        customRules = latest(local.customRules, incoming.customRules),
        skipCompleted = latest(local.skipCompleted, incoming.skipCompleted),
        skipUnseen = latest(local.skipUnseen, incoming.skipUnseen),
        skipNotStarted = latest(local.skipNotStarted, incoming.skipNotStarted),
        skipOutsideReleasePeriod = latest(local.skipOutsideReleasePeriod, incoming.skipOutsideReleasePeriod),
        intervalHours = latest(local.intervalHours, incoming.intervalHours),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: CategoryUpdateRulesPayload): ApplyOutcome {
        val profileId = writes.profileId(payload.profileUuid).idOr { return it }
        val categoryId = writes.categoryId(payload.categoryKey, payload.profileUuid).idOr { return it }
        writes.database.sync_applyQueries.upsertCategoryUpdateRules(
            profileId = profileId,
            categoryId = categoryId,
            autoUpdate = payload.autoUpdate.value,
            customRules = payload.customRules.value,
            skipCompleted = payload.skipCompleted.value,
            skipUnseen = payload.skipUnseen.value,
            skipNotStarted = payload.skipNotStarted.value,
            skipOutsideReleasePeriod = payload.skipOutsideReleasePeriod.value,
            intervalHours = payload.intervalHours.value,
        )
        return ApplyOutcome.Applied(LocalRef.of(profileId, categoryId))
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.sync_applyQueries.deleteCategoryUpdateRules(it.long(0), it.long(1)) }
        return ApplyOutcome.Applied(null)
    }
}
