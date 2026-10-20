package mihon.entry.interactions.library

import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned projection for library-update notification routing, vocabulary, and actions. */
interface EntryLibraryUpdateNotificationFeature {
    /** Resolves whether metered-source queue concentration requires the shared warning. */
    suspend fun queueWarning(entries: List<Entry>): EntryLibraryUpdateQueueWarning

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
    val summary: EntryLibraryUpdateNotificationSummary,
    val updates: List<EntryLibraryUpdateNotificationItem>,
)

data class EntryLibraryUpdateNotificationItem(
    val originEntry: Entry,
    val visibleEntry: Entry,
    val children: List<EntryChapter>,
    val description: EntryLibraryUpdateNotificationText,
    /** Set when another entry in the same group has the same title, so the type tells the two apart. */
    val typeLabel: StringResource?,
    val destination: EntryLibraryUpdateNotificationDestination,
    val actions: Set<EntryLibraryUpdateNotificationAction>,
    val markConsumedLabel: StringResource,
    val viewChildrenLabel: StringResource,
)

/**
 * Wording of the summary over one update's group. Every participating content type shares it, so mixed-type
 * updates surface as a single notification; the app gives each profile its own channel, group and summary.
 */
data class EntryLibraryUpdateNotificationSummary(
    val title: StringResource,
    val text: PluralsResource,
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
