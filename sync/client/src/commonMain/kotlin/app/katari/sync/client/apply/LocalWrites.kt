package app.katari.sync.client.apply

import app.katari.sync.client.payload.SourceReference
import app.katari.sync.client.setting.apply.SettingWrites
import app.katari.sync.protocol.clock.HybridTimestamp
import tachiyomi.data.Database

/**
 * What a kind writes an incoming record with, inside the transaction that applies it: the database, the preference
 * stores, and the local IDs of the records the record refers to.
 *
 * A record refers to others by key. Its parent's local ID comes from the record this device stores for the parent;
 * when that record is here but its row is not, such as an entry whose row was removed as cache, the row is written
 * from the stored record first. A parent with no record here leaves the record waiting for it.
 */
internal interface LocalWrites {
    val database: Database
    val settings: SettingWrites

    /** The profile open on this device, which sync never deletes from under the person. */
    val activeProfileId: Long

    suspend fun profileId(uuid: String): Resolved

    suspend fun entryId(entryKey: String): Resolved

    suspend fun chapterId(chapterKey: String): Resolved

    /** The category [categoryKey] of a profile, the Default one (0) included, following categories merged by name. */
    suspend fun categoryId(categoryKey: String, profileUuid: String): Resolved

    /** [Resolved.Found] with 0 when the session's row is here, since sessions are named by their own ID. */
    suspend fun activitySession(sessionId: String): Resolved

    /** This device's key for [source], registering it when no extension here has provided it yet. */
    suspend fun sourceKey(source: SourceReference): Long

    /** The record key of the entry that replaced [entryKey], when one did. */
    suspend fun replacementOf(entryKey: String): String?

    /**
     * A timestamp for a change sync makes on this device's behalf, such as merging two categories into one: after
     * every edit this device has made or seen, as a person's edit made now would be.
     */
    suspend fun newEdit(): HybridTimestamp
}
