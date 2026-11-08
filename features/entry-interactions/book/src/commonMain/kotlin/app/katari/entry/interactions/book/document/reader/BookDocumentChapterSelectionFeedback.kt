package app.katari.entry.interactions.book.document.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.katari.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import app.katari.entry.interactions.book.reader.BookOpeningLoadingScreen
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Error
import app.katari.icons.materialsymbols.rounded.Refresh
import app.katari.presentation.kit.feedback.ProblemAction
import app.katari.presentation.kit.feedback.ProblemCard
import app.katari.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Feedback for a chapter opened from the contents or the seek controls. Such a chapter is often
 * not adjacent to the current one, so no transition row exists to show its loading or failure.
 * Both replace the page the reader is leaving; dismissing a failure goes back to that page.
 */
@Composable
internal fun BookDocumentChapterSelectionFeedback(
    selection: BookDocumentChapterSelectionLoad,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val background = Modifier
        .fillMaxSize()
        .background(LocalBookDocumentReaderPalette.current.background)
    when (val loadState = selection.loadState) {
        BookDocumentChapterLoadState.Loading -> BookOpeningLoadingScreen(
            progress = selection.progress,
            description = stringResource(MR.strings.loading),
            modifier = background,
        )
        is BookDocumentChapterLoadState.Failed -> {
            NavigationBackHandler(
                state = rememberNavigationEventState(NavigationEventInfo.None),
                onBackCompleted = onDismiss,
            )
            Box(modifier = background, contentAlignment = Alignment.Center) {
                ProblemCard(
                    icon = MaterialSymbols.Rounded.Error,
                    heading = stringResource(MR.strings.transition_chapter_failed),
                    message = loadState.message,
                    actions = listOf(
                        ProblemAction(
                            stringResource(MR.strings.action_retry),
                            onRetry,
                            MaterialSymbols.Rounded.Refresh,
                        ),
                        ProblemAction(stringResource(MR.strings.action_cancel), onDismiss),
                    ),
                    modifier = Modifier
                        .widthIn(max = KatariTheme.sizes.readableWidth)
                        .padding(KatariTheme.spacing.xl),
                )
            }
        }
    }
}
