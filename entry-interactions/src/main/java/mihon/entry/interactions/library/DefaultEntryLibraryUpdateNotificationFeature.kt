package mihon.entry.interactions.library

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnmeteredSource
import mihon.entry.interactions.download.EntryDownloadActionAvailability
import mihon.entry.interactions.download.EntryDownloadActionFeature
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.presentation.EntryTypePresentationResult
import mihon.entry.interactions.state.EntryConsumptionFeature
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.service.SourceManager

internal class DefaultEntryLibraryUpdateNotificationFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val presentationFeature: EntryTypePresentationFeature,
    private val openFeature: EntryOpenFeature,
    private val consumptionFeature: EntryConsumptionFeature,
    private val downloadActionFeature: EntryDownloadActionFeature,
    private val sourceManager: SourceManager,
    private val resolveVisibleEntry: suspend (Entry) -> Entry,
    private val queueWarningThreshold: Int = 60,
) : EntryLibraryUpdateNotificationFeature {
    private val participatingTypes = evaluation.libraryUpdateNotificationTypes(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_BASE_INTEGRATION_ID,
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_ROUTE_BEHAVIOR_ID,
    )
    private val renderTypes = evaluation.libraryUpdateNotificationTypes(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_BASE_INTEGRATION_ID,
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_RENDER_BEHAVIOR_ID,
    )
    private val queueWarningTypes = evaluation.libraryUpdateNotificationTypes(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_BASE_INTEGRATION_ID,
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_QUEUE_WARNING_BEHAVIOR_ID,
    )
    private val contributedPresentationTypes = evaluation.libraryUpdateNotificationTypes(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_PRESENTATION_INTEGRATION_ID,
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_PRESENTATION_BEHAVIOR_ID,
    )
    private val openTypes = evaluation.libraryUpdateNotificationTypes(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_INTEGRATION_ID,
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_BEHAVIOR_ID,
    )
    private val consumptionTypes = evaluation.libraryUpdateNotificationTypes(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_INTEGRATION_ID,
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_BEHAVIOR_ID,
    )
    private val downloadTypes = evaluation.libraryUpdateNotificationTypes(
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_DOWNLOAD_INTEGRATION_ID,
        ENTRY_LIBRARY_UPDATE_NOTIFICATION_DOWNLOAD_BEHAVIOR_ID,
    )

    init {
        check(participatingTypes == renderTypes) {
            "Library-update notification routing and rendering selected different content types"
        }
        check(participatingTypes == queueWarningTypes) {
            "Library-update notification routing and queue warnings selected different content types"
        }
        require(queueWarningThreshold >= 0) { "Library-update queue warning threshold must be non-negative" }
    }

    override fun routes(): List<EntryLibraryUpdateNotificationRoute> = listOf(
        sharedLibraryUpdateNotificationRoute,
    )

    override fun queueWarning(entries: List<Entry>): EntryLibraryUpdateQueueWarning {
        entries.forEach { entry ->
            check(entry.type in queueWarningTypes) {
                "Entry type ${entry.type} was not contributed to Library Update notifications"
            }
        }
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
        val items = participatingUpdates.map { buildNotificationItem(it) }
        val groups = if (items.isEmpty()) {
            emptyList()
        } else {
            listOf(
                EntryLibraryUpdateNotificationGroup(
                    route = sharedLibraryUpdateNotificationRoute,
                    updates = items,
                ),
            )
        }
        return EntryLibraryUpdateNotificationProjection(groups = groups, omissions = omissions)
    }

    private suspend fun buildNotificationItem(
        update: EntryLibraryUpdateNotificationInput,
    ): EntryLibraryUpdateNotificationItem {
        val visibleEntry = resolveVisibleEntry(update.entry)
        check(visibleEntry.type == update.entry.type) {
            "Visible notification target ${visibleEntry.id} has type ${visibleEntry.type}, " +
                "but origin ${update.entry.id} has type ${update.entry.type}"
        }
        val type = visibleEntry.type
        val presentationResult = presentationFeature.presentation(type)
        checkPresentationRelationship(type, presentationResult)
        val presentation = presentationResult.presentation
        val vocabulary = presentation.updateNotification
        val hasChildren = update.children.isNotEmpty()
        val destination = if (type in openTypes) {
            evaluation.requireLibraryUpdateNotificationOpenContext(type, hasChildren)
            if (hasChildren) {
                check(openFeature.isApplicable(type)) {
                    "Library-update notifications selected Open for $type, but Open rejected it"
                }
                EntryLibraryUpdateNotificationDestination.OPEN_CHILD
            } else {
                EntryLibraryUpdateNotificationDestination.ENTRY_DETAILS
            }
        } else {
            EntryLibraryUpdateNotificationDestination.ENTRY_DETAILS
        }
        val actions = buildSet {
            add(EntryLibraryUpdateNotificationAction.VIEW_ENTRY)
            if (type in consumptionTypes) {
                evaluation.requireLibraryUpdateNotificationConsumptionContext(type, hasChildren)
                if (hasChildren) {
                    check(consumptionFeature.isApplicable(type)) {
                        "Library-update notifications selected Consumption for $type, but Consumption rejected it"
                    }
                    add(EntryLibraryUpdateNotificationAction.MARK_CONSUMED)
                }
            }
            if (type in downloadTypes) {
                val availability = downloadActionFeature.notificationAvailability(
                    entry = update.entry,
                    childCount = update.children.size,
                )
                when (availability) {
                    EntryDownloadActionAvailability.Available -> {
                        add(EntryLibraryUpdateNotificationAction.DOWNLOAD)
                    }

                    is EntryDownloadActionAvailability.Blocked -> Unit
                    is EntryDownloadActionAvailability.Inapplicable -> error(
                        "Library-update notifications selected Download for $type, but Download rejected it",
                    )
                }
            }
        }
        return EntryLibraryUpdateNotificationItem(
            originEntry = update.entry,
            visibleEntry = visibleEntry,
            children = update.children,
            description = vocabulary.describeLibraryUpdate(update.children),
            destination = destination,
            actions = actions,
            markConsumedLabel = presentation.markAsConsumedLabel,
            viewChildrenLabel = vocabulary.viewChildrenLabel,
        )
    }

    private fun checkPresentationRelationship(type: EntryType, result: EntryTypePresentationResult) {
        when (result) {
            is EntryTypePresentationResult.Contributed -> check(type in contributedPresentationTypes) {
                "Type Presentation returned contributed vocabulary for $type without the notification relationship"
            }
            is EntryTypePresentationResult.Generic -> check(type !in contributedPresentationTypes) {
                "Notification presentation selected contributed vocabulary for $type, but received generic vocabulary"
            }
        }
    }
}
