package app.katari.sync.client.kind.extension

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.merge.larger
import app.katari.sync.client.merge.latest
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.SyncedKind

/**
 * Extension repositories, keyed by index URL. Local ref: `[index_url]`. Adding and removing one is captured;
 * refreshing its index is not. Which extensions each device installed is [InstalledExtensionKind].
 *
 * Version 1 carried no time, so a removal won over every later add of the same repository and a device that still
 * held the removal deleted it again everywhere. Its records read as added at an unknown time.
 */
object ExtensionRepositoryKind : SyncedKind<ExtensionRepositoryPayload>(
    "extension.repository",
    2,
    ExtensionRepositoryPayload.serializer(),
    Absence.TOMBSTONE,
) {
    override val oldestReadableVersion = 1

    override val dataGroup = DataGroup.EXTENSION_REPOSITORIES

    override val capture = listOf(
        CaptureTrigger("extension_store", Event.INSERT, listOf("NEW.index_url")),
        CaptureTrigger("extension_store", Event.DELETE, listOf("OLD.index_url")),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<ExtensionRepositoryPayload> {
        val row = reads.database.sync_local_rowsQueries.extensionRepositoryRow(ref.text(0)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        return LocalRead.Found(RecordKeys.derive(name, row.index_url)) { previous, stamps ->
            ExtensionRepositoryPayload(
                indexUrl = row.index_url,
                name = row.name,
                badgeLabel = row.badge_label,
                signingKey = row.signing_key,
                contactWebsite = row.contact_website,
                contactDiscord = row.contact_discord,
                format = row.format,
                extensionListUrl = row.extension_list_url,
                added = stamps.field(ADDED, true, previous?.added),
            )
        }
    }

    /**
     * The later add; everything else but the URL comes from the repository's index, which each device fetches, so
     * any copy will do.
     */
    override fun merge(
        local: ExtensionRepositoryPayload,
        incoming: ExtensionRepositoryPayload,
    ): ExtensionRepositoryPayload {
        val added = latest(local.added, incoming.added)
        return larger(local.copy(added = added), incoming.copy(added = added))
    }

    override suspend fun apply(writes: LocalWrites, key: String, payload: ExtensionRepositoryPayload): ApplyOutcome {
        writes.database.sync_applyQueries.insertExtensionRepository(
            indexUrl = payload.indexUrl,
            name = payload.name,
            badgeLabel = payload.badgeLabel,
            signingKey = payload.signingKey,
            contactWebsite = payload.contactWebsite,
            contactDiscord = payload.contactDiscord,
            format = payload.format,
            extensionListUrl = payload.extensionListUrl,
        )
        return ApplyOutcome.Applied(LocalRef.of(payload.indexUrl))
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.sync_applyQueries.deleteExtensionRepository(it.text(0)) }
        return ApplyOutcome.Applied(null)
    }

    private const val ADDED = 9
}
