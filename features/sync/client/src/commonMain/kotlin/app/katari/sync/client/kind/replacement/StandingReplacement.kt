package app.katari.sync.client.kind.replacement

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.protocol.clock.HybridTimestamp
import tachiyomi.data.Database

/**
 * The replacement of an entry this device stores, as it stands with the entry's own record: [returnedAt] is when the
 * person brought the entry back as itself after it ([EntryReplacementPayload.returnedAt]), and null while it holds.
 * Once the entry is back, its record writes its own row again and its edits stay with it; only what came before its
 * return still belongs to the entry that replaced it.
 */
internal class StandingReplacement(val payload: EntryReplacementPayload, val returnedAt: HybridTimestamp?) {
    val replacement: Replacement get() = payload.replacement.value
    val holds: Boolean get() = returnedAt == null
}

internal suspend fun Database.standingReplacement(entryKey: String): StandingReplacement? {
    val stored = sync_recordsQueries.getByKey(EntryReplacementKind.key(entryKey)).awaitAsOneOrNull()
        ?.takeUnless { it.deleted }
        ?: return null
    val payload = EntryReplacementKind.decode(stored.payload)
    val entry = sync_recordsQueries.getByKey(entryKey).awaitAsOneOrNull()
        ?.takeUnless { it.deleted }
        ?.let { EntryKind.decode(it.payload) }
    return StandingReplacement(payload, payload.returnedAt(entry))
}

/** The record key of the entry that replaced [entryKey], while that replacement holds. */
internal suspend fun LocalWrites.replacementOf(entryKey: String): String? =
    database.standingReplacement(entryKey)?.takeIf { it.holds }?.replacement?.to

/** The record key of the entry [entryKey] became by moving to another profile, while it hasn't moved back. */
internal suspend fun LocalWrites.movedTo(entryKey: String): String? = database.standingReplacement(entryKey)
    ?.takeIf { it.holds && it.replacement.cause == Replacement.Cause.PROFILE_MOVE }
    ?.replacement
    ?.to
