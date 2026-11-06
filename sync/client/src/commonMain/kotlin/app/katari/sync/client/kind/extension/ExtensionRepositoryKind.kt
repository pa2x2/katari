package app.katari.sync.client.kind.extension

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.merge.larger
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.SyncedKind

/**
 * Extension repositories, keyed by index URL. Local ref: `[index_url]`. Adding and removing one is captured;
 * refreshing its index is not. Installed extensions never sync.
 */
object ExtensionRepositoryKind : SyncedKind<ExtensionRepositoryPayload>(
    "extension.repository",
    1,
    ExtensionRepositoryPayload.serializer(),
    Absence.TOMBSTONE,
) {
    override val capture = listOf(
        CaptureTrigger("extension_store", Event.INSERT, listOf("NEW.index_url")),
        CaptureTrigger("extension_store", Event.DELETE, listOf("OLD.index_url")),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<ExtensionRepositoryPayload> {
        val row = reads.database.sync_local_rowsQueries.extensionRepositoryRow(ref.text(0)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        return LocalRead.Found(RecordKeys.derive(name, row.index_url)) { _, _ ->
            ExtensionRepositoryPayload(
                indexUrl = row.index_url,
                name = row.name,
                badgeLabel = row.badge_label,
                signingKey = row.signing_key,
                contactWebsite = row.contact_website,
                contactDiscord = row.contact_discord,
                format = row.format,
                extensionListUrl = row.extension_list_url,
            )
        }
    }

    /** Everything but the URL comes from the repository's index, which each device fetches; any copy will do. */
    override fun merge(local: ExtensionRepositoryPayload, incoming: ExtensionRepositoryPayload) =
        larger(local, incoming)
}
