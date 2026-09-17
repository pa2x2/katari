package mihon.entry.interactions.library

import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned projection for library-update notification routing, vocabulary, and actions. */
interface EntryLibraryUpdateNotificationFeature {
    /** The single shared route used by every participating content type. */
    fun routes(): List<EntryLibraryUpdateNotificationRoute>

    /** Resolves whether metered-source queue concentration requires the shared warning. */
    fun queueWarning(entries: List<Entry>): EntryLibraryUpdateQueueWarning

    /** Converts actual library-update participation into the complete shared notification render plan. */
    suspend fun project(
        updates: List<EntryLibraryUpdateNotificationInput>,
    ): EntryLibraryUpdateNotificationProjection
}

sealed interface EntryLibraryUpdateQueueWarning {
    data object NotRequired : EntryLibraryUpdateQueueWarning

    data class Required(val maxEntriesPerMeteredSource: Int) : EntryLibraryUpdateQueueWarning
}

data class EntryLibraryUpdateNotificationInput(
    val entry: Entry,
    val children: List<EntryChapter>,
)

data class EntryLibraryUpdateNotificationProjection(
    val groups: List<EntryLibraryUpdateNotificationGroup>,
    val omissions: List<EntryLibraryUpdateNotificationOmission>,
)

data class EntryLibraryUpdateNotificationGroup(
    val route: EntryLibraryUpdateNotificationRoute,
    val updates: List<EntryLibraryUpdateNotificationItem>,
)

data class EntryLibraryUpdateNotificationItem(
    val originEntry: Entry,
    val visibleEntry: Entry,
    val children: List<EntryChapter>,
    val description: EntryLibraryUpdateNotificationText,
    val destination: EntryLibraryUpdateNotificationDestination,
    val actions: Set<EntryLibraryUpdateNotificationAction>,
    val markConsumedLabel: StringResource,
    val viewChildrenLabel: StringResource,
)

/**
 * Single shared Android identity for library-update notifications, independent of content type.
 *
 * Every participating content type posts into this one channel, group, and summary, so mixed-type
 * library updates surface as a single notification.
 */
data class EntryLibraryUpdateNotificationRoute(
    val channelId: String,
    val channelLabel: StringResource,
    val groupKey: String,
    val summaryNotificationId: Int,
    val summaryTitle: StringResource,
    val summaryText: PluralsResource,
)

enum class EntryLibraryUpdateNotificationDestination {
    OPEN_CHILD,
    ENTRY_DETAILS,
}

enum class EntryLibraryUpdateNotificationAction {
    MARK_CONSUMED,
    VIEW_ENTRY,
    DOWNLOAD,
}

sealed interface EntryLibraryUpdateNotificationText {
    data class StringText(
        val resource: StringResource,
        val arguments: List<Any> = emptyList(),
    ) : EntryLibraryUpdateNotificationText

    data class PluralText(
        val resource: PluralsResource,
        val quantity: Int,
        val arguments: List<Any> = emptyList(),
    ) : EntryLibraryUpdateNotificationText
}

data class EntryLibraryUpdateNotificationOmission(
    val type: EntryType,
    val updateCount: Int,
    val reason: EntryLibraryUpdateNotificationOmissionReason,
)

enum class EntryLibraryUpdateNotificationOmissionReason {
    NOT_AN_UPDATE_PARTICIPANT,
}
