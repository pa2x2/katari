package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnmeteredSource
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
    private val routesByType = participatingTypes
        .associateWith(::createRoute)
        .also(::validateLibraryUpdateNotificationRoutes)

    init {
        check(participatingTypes == renderTypes) {
            "Library-update notification routing and rendering selected different content types"
        }
        check(participatingTypes == queueWarningTypes) {
            "Library-update notification routing and queue warnings selected different content types"
        }
        require(queueWarningThreshold >= 0) { "Library-update queue warning threshold must be non-negative" }
    }

    override fun routes(): List<EntryLibraryUpdateNotificationRoute> = routesByType.values.toList()

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
        val groups = updates.groupBy { it.entry.type }.mapNotNull { (type, typeUpdates) ->
            val route = routesByType[type]
            if (route == null) {
                omissions += EntryLibraryUpdateNotificationOmission(
                    type = type,
                    updateCount = typeUpdates.size,
                    reason = EntryLibraryUpdateNotificationOmissionReason.NOT_AN_UPDATE_PARTICIPANT,
                )
                return@mapNotNull null
            }

            val presentationResult = presentationFeature.presentation(type)
            checkPresentationRelationship(type, presentationResult)
            val presentation = presentationResult.presentation
            val vocabulary = presentation.updateNotification
            EntryLibraryUpdateNotificationGroup(
                route = route.copy(channelLabel = vocabulary.channelLabel),
                summaryTitle = vocabulary.summaryTitle,
                summaryText = vocabulary.summaryText,
                updates = typeUpdates.map { update ->
                    val visibleEntry = resolveVisibleEntry(update.entry)
                    check(visibleEntry.type == update.entry.type) {
                        "Visible notification target ${visibleEntry.id} has type ${visibleEntry.type}, " +
                            "but origin ${update.entry.id} has type ${update.entry.type}"
                    }
                    val hasChildren = update.children.isNotEmpty()
                    val destination = if (visibleEntry.type in openTypes) {
                        evaluation.requireLibraryUpdateNotificationOpenContext(visibleEntry.type, hasChildren)
                        if (hasChildren) {
                            check(openFeature.isApplicable(visibleEntry.type)) {
                                "Library-update notifications selected Open for ${visibleEntry.type}, " +
                                    "but Open rejected it"
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
                        if (update.entry.type in consumptionTypes) {
                            evaluation.requireLibraryUpdateNotificationConsumptionContext(
                                update.entry.type,
                                hasChildren,
                            )
                            if (hasChildren) {
                                check(consumptionFeature.isApplicable(update.entry.type)) {
                                    "Library-update notifications selected Consumption for ${update.entry.type}, " +
                                        "but Consumption rejected it"
                                }
                                add(EntryLibraryUpdateNotificationAction.MARK_CONSUMED)
                            }
                        }
                        if (update.entry.type in downloadTypes) {
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
                                    "Library-update notifications selected Download for ${update.entry.type}, " +
                                        "but Download rejected it",
                                )
                            }
                        }
                    }
                    EntryLibraryUpdateNotificationItem(
                        originEntry = update.entry,
                        visibleEntry = visibleEntry,
                        children = update.children,
                        description = vocabulary.describeLibraryUpdate(update.children),
                        destination = destination,
                        actions = actions,
                        markConsumedLabel = presentation.markAsConsumedLabel,
                        viewChildrenLabel = vocabulary.viewChildrenLabel,
                    )
                },
            )
        }
        return EntryLibraryUpdateNotificationProjection(groups = groups, omissions = omissions)
    }

    private fun createRoute(type: EntryType): EntryLibraryUpdateNotificationRoute {
        val legacy = LegacyLibraryUpdateNotificationRouteCompatibility.route(type)
        val typeKey = type.toContentTypeId().value
        val presentationResult = presentationFeature.presentation(type)
        checkPresentationRelationship(type, presentationResult)
        val presentation = presentationResult.presentation.updateNotification
        return EntryLibraryUpdateNotificationRoute(
            type = type,
            channelId = legacy?.channelId ?: "entry_library_updates_${typeKey}_channel",
            channelLabel = presentation.channelLabel,
            groupKey = legacy?.groupKey ?: "mihon.entry.library_updates.$typeKey",
            summaryNotificationId = legacy?.summaryNotificationId
                ?: derivedLibraryUpdateSummaryNotificationId(typeKey),
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
