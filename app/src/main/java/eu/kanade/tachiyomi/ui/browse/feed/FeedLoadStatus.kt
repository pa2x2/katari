package eu.kanade.tachiyomi.ui.browse.feed

import android.content.Context
import eu.kanade.domain.source.model.FilterPresetRepairException
import eu.kanade.domain.source.model.SourceFeedGap
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationException
import tachiyomi.i18n.*
import tachiyomi.core.common.i18n.stringResource as coreStringResource

internal fun Throwable.requiresFilterRepair(): Boolean {
    return this is FilterPresetRepairException || this is EntryFilterValidationException
}

internal fun Context.feedErrorMessage(error: Throwable): String {
    if (error.requiresFilterRepair()) return coreStringResource(MR.strings.filter_repair_help)

    // For errors it has no wording of its own for, the formatter prefixes the exception's class name, which reads
    // like a crash. A source's own message is written for the reader, so it is shown as is.
    val formatted = error.formattedMessage
    val message = error.message
    return if (!message.isNullOrBlank() && formatted == "${error::class.simpleName}: $message") message else formatted
}

internal fun FeedViewModel.State.gapStatus(gap: SourceFeedGap, context: Context): FeedGapStatus {
    val load = gapLoad
    val failure = gapError
    return when {
        load != null && load.after == gap.after -> FeedGapStatus.Loading(load.pagesLoaded)
        failure != null && failure.gap == gap -> FeedGapStatus.Failed(context.feedErrorMessage(failure.error))
        else -> FeedGapStatus.Idle
    }
}
