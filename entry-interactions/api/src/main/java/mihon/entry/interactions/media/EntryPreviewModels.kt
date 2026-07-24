package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

data class EntryPreviewConfig(
    val enabled: Boolean,
    val pageCount: Int,
    val size: EntryPreviewSize,
) {
    companion object {
        val Default = EntryPreviewConfig(
            enabled = true,
            pageCount = 5,
            size = EntryPreviewSize.MEDIUM,
        )

        val Disabled = EntryPreviewConfig(
            enabled = false,
            pageCount = 0,
            size = EntryPreviewSize.MEDIUM,
        )
    }
}

data class EntryPreviewSettings(
    val type: EntryType,
    val enabled: Preference<Boolean>,
    val pageCount: Preference<Int>,
    val size: Preference<EntryPreviewSize>,
    val sourceRequirement: EntryPreviewSourceRequirement = EntryPreviewSourceRequirement.NONE,
)

enum class EntryPreviewSourceRequirement {
    NONE,
    PREVIEW_CAPABILITY,
}

data class EntryPreviewContext(
    val entry: Entry,
    val source: UnifiedSource,
)

data class EntryPreviewChildCandidate(
    val entry: Entry,
    val child: EntryChapter,
    val source: UnifiedSource,
)

enum class EntryPreviewSize {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE,
}

data class EntryPreviewHandle(
    val entryType: EntryType,
    val chapterId: Long?,
    val pages: List<EntryPreviewPage>,
    val delegate: Any? = null,
)

data class EntryPreviewPage(
    val index: Int,
    val status: StateFlow<EntryPreviewPageStatus>,
    val progress: StateFlow<Int>,
    val imageModel: Any,
    val canOpen: Boolean = true,
)

sealed interface EntryPreviewPageStatus {
    data object Queued : EntryPreviewPageStatus
    data object LoadingPage : EntryPreviewPageStatus
    data object DownloadingImage : EntryPreviewPageStatus
    data object Ready : EntryPreviewPageStatus
    data class Error(val error: Throwable) : EntryPreviewPageStatus
}

sealed interface EntryPreviewAvailability {
    val config: EntryPreviewConfig

    data class Available(override val config: EntryPreviewConfig) : EntryPreviewAvailability

    data class Disabled(override val config: EntryPreviewConfig) : EntryPreviewAvailability

    data class ContextuallyUnavailable(
        override val config: EntryPreviewConfig,
        val reason: EntryPreviewUnavailableReason,
    ) : EntryPreviewAvailability

    data class Inapplicable(
        val type: EntryType,
        override val config: EntryPreviewConfig = EntryPreviewConfig.Disabled,
    ) : EntryPreviewAvailability
}

sealed interface EntryPreviewUnavailableReason {
    data object SourceUnsupported : EntryPreviewUnavailableReason
    data object NoReadingChild : EntryPreviewUnavailableReason
}

data class EntryPreviewLoadRequest(
    val context: android.content.Context,
    val previewContext: EntryPreviewContext,
    val children: List<EntryPreviewChildCandidate>,
    val memberIds: List<Long>,
)

sealed interface EntryPreviewLoadResult {
    data class Loaded(val handle: EntryPreviewHandle) : EntryPreviewLoadResult
    data class Disabled(val config: EntryPreviewConfig) : EntryPreviewLoadResult
    data class ContextuallyUnavailable(val reason: EntryPreviewUnavailableReason) : EntryPreviewLoadResult
    data class Inapplicable(val type: EntryType) : EntryPreviewLoadResult
}

sealed interface EntryPreviewOpenTargetResult {
    data class Available(val childId: Long, val pageIndex: Int) : EntryPreviewOpenTargetResult
    data object NotOpenable : EntryPreviewOpenTargetResult
    data class Inapplicable(val type: EntryType) : EntryPreviewOpenTargetResult
}
