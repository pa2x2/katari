package eu.kanade.tachiyomi.entry

import mihon.entry.interactions.EntryMetadataLifecycleFeature
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryMetadataChangeNotifier

class AppEntryMetadataChangeNotifier(
    private val metadataLifecycle: EntryMetadataLifecycleFeature,
) : EntryMetadataChangeNotifier {
    override suspend fun changed(previous: Entry, current: Entry) {
        metadataLifecycle.changed(previous, current)
    }
}
