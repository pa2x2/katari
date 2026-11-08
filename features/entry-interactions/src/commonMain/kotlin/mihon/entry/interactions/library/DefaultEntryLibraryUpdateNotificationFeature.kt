package mihon.entry.interactions.library

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnmeteredSource
import mihon.entry.interactions.download.EntryDownloadActionAvailability
import mihon.entry.interactions.download.EntryDownloadActionFeature
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.state.EntryConsumptionFeature
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.awaitInitialized

internal class DefaultEntryLibraryUpdateNotificationFeature(
    private val participatingTypes: Set<EntryType>,
    private val presentationFeature: EntryTypePresentationFeature,
    private val openFeature: EntryOpenFeature,
    private val consumptionFeature: EntryConsumptionFeature,
    private val downloadActionFeature: EntryDownloadActionFeature,
    private val sourceManager: SourceManager,
    private val resolveVisibleEntry: suspend (Entry) -> Entry,
    private val queueWarningThreshold: Int = 60,
) : EntryLibraryUpdateNotificationFeature {
    init {
        require(queueWarningThreshold >= 0) { "Library-update queue warning threshold must be non-negative" }
    }

    override suspend fun queueWarning(entries: List<Entry>): EntryLibraryUpdateQueueWarning {
        // Sources that haven't loaded yet would all count as metered
        sourceManager.awaitInitialized()
        val maxEntriesPerMeteredSource = entries
            .groupBy(Entry::source)
            .filterKeys { sourceId -> sourceManager.get(sourceId) !is UnmeteredSource }
            .maxOfOrNull { (_, sourceEntries) -> sourceEntries.size }
            ?: 0
        return if (maxEntriesPerMeteredSource > queueWarningThreshold) {
            EntryLibraryUpdateQueueWarning.Required(maxEntriesPerMeteredSource)
        } else {
            EntryLibraryUpdateQueueWarning.NotRequired
        }
    }

    override suspend fun project(
        updates: List<EntryLibraryUpdateNotificationInput>,
    ): EntryLibraryUpdateNotificationProjection {
        val omissions = mutableListOf<EntryLibraryUpdateNotificationOmission>()
        val participatingUpdates = mutableListOf<EntryLibraryUpdateNotificationInput>()
        updates.groupBy { it.entry.type }.forEach { (type, typeUpdates) ->
            if (type in participatingTypes) {
                participatingUpdates += typeUpdates
            } else {
                omissions += EntryLibraryUpdateNotificationOmission(
                    type = type,
                    updateCount = typeUpdates.size,
                    reason = EntryLibraryUpdateNotificationOmissionReason.NOT_AN_UPDATE_PARTICIPANT,
                )
            }
        }
        // Only the type tells such entries apart in the shade, as a book and a manga of one series.
        val sharedTitles = participatingUpdates.groupBy { it.entry.displayTitle }
            .filterValues { sameTitle -> sameTitle.distinctBy { it.entry.type }.size > 1 }
            .keys
        val items = participatingUpdates.map {
            buildNotificationItem(
                it,
                labelType =
                it.entry.displayTitle in sharedTitles,
            )
        }
        val groups = if (items.isEmpty()) {
            emptyList()
        } else {
            listOf(
                EntryLibraryUpdateNotificationGroup(
                    summary = sharedLibraryUpdateNotificationSummary,
                    updates = items,
                ),
            )
        }
        return EntryLibraryUpdateNotificationProjection(groups = groups, omissions = omissions)
    }

    private suspend fun buildNotificationItem(
        update: EntryLibraryUpdateNotificationInput,
        labelType: Boolean,
    ): EntryLibraryUpdateNotificationItem {
        val visibleEntry = resolveVisibleEntry(update.entry)
        check(visibleEntry.type == update.entry.type) {
            "Visible notification target ${visibleEntry.id} has type ${visibleEntry.type}, " +
                "but origin ${update.entry.id} has type ${update.entry.type}"
        }
        val type = visibleEntry.type
        val presentation = presentationFeature.presentation(type).presentation
        val vocabulary = presentation.updateNotification
        val hasChildren = update.children.isNotEmpty()
        val destination = if (hasChildren && openFeature.isApplicable(type)) {
            EntryLibraryUpdateNotificationDestination.OPEN_CHILD
        } else {
            EntryLibraryUpdateNotificationDestination.ENTRY_DETAILS
        }
        val actions = buildSet {
            add(EntryLibraryUpdateNotificationAction.VIEW_ENTRY)
            if (hasChildren && consumptionFeature.isApplicable(type)) {
                add(EntryLibraryUpdateNotificationAction.MARK_CONSUMED)
            }
            val download = downloadActionFeature.notificationAvailability(
                entry = update.entry,
                childCount = update.children.size,
            )
            if (download == EntryDownloadActionAvailability.Available) {
                add(EntryLibraryUpdateNotificationAction.DOWNLOAD)
            }
        }
        return EntryLibraryUpdateNotificationItem(
            originEntry = update.entry,
            visibleEntry = visibleEntry,
            children = update.children,
            description = vocabulary.describeLibraryUpdate(update.children),
            typeLabel = presentation.displayNameLabel.takeIf { labelType },
            destination = destination,
            actions = actions,
            markConsumedLabel = presentation.markAsConsumedLabel,
            viewChildrenLabel = vocabulary.viewChildrenLabel,
        )
    }
}
