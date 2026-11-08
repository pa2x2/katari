package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.katari.core.platform.PlatformContext
import app.katari.core.platform.currentPlatformContext
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.domain.source.model.FilterPresetRepairException
import eu.kanade.domain.source.model.SourceFeedGap
import eu.kanade.presentation.util.SourceErrorMessages
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationException
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource as coreStringResource

internal fun Throwable.requiresFilterRepair(): Boolean {
    return this is FilterPresetRepairException || this is EntryFilterValidationException
}

internal fun SourceErrorMessages.feedErrorMessage(context: PlatformContext, error: Throwable): String {
    if (error.requiresFilterRepair()) return context.coreStringResource(MR.strings.filter_repair_help)

    // For errors it has no wording of its own for, the formatter prefixes the exception's class name, which reads
    // like a crash. A source's own message is written for the reader, so it is shown as is.
    val formatted = message(context, error)
    val ownMessage = error.message
    return if (!ownMessage.isNullOrBlank() && formatted == "${error::class.simpleName}: $ownMessage") {
        ownMessage
    } else {
        formatted
    }
}

/** Words a feed's load failures as [feedErrorMessage] does, for the screen it's called from. */
@Composable
internal fun rememberFeedErrorMessage(): (Throwable) -> String {
    val context = currentPlatformContext()
    val errorMessages = LocalAppUiGraph.current.sourceErrorMessages
    return remember(context, errorMessages) { { error -> errorMessages.feedErrorMessage(context, error) } }
}

internal fun FeedViewModel.State.gapStatus(gap: SourceFeedGap, errorMessage: (Throwable) -> String): FeedGapStatus {
    val load = gapLoad
    val failure = gapError
    return when {
        load != null && load.after == gap.after -> FeedGapStatus.Loading(load.pagesLoaded)
        failure != null && failure.gap == gap -> FeedGapStatus.Failed(errorMessage(failure.error))
        else -> FeedGapStatus.Idle
    }
}
