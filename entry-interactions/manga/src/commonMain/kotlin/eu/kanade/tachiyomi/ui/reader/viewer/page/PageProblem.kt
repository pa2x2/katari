package eu.kanade.tachiyomi.ui.reader.viewer.page

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.readerFormattedMessage
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Error
import mihon.icons.materialsymbols.rounded.OpenInBrowser
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.presentation.kit.feedback.ProblemAction
import mihon.presentation.kit.feedback.ProblemCard
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Why [page] failed to show, with a retry and, when the page has a web address, a way to open it in the browser. */
@Composable
internal fun PageProblem(
    page: ReaderPage,
    error: Throwable?,
    onOpenInWebView: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ProblemCard(
        icon = MaterialSymbols.Rounded.Error,
        heading = stringResource(MR.strings.decode_image_error),
        message = error?.readerFormattedMessage.orEmpty(),
        actions = listOfNotNull(
            ProblemAction(
                stringResource(MR.strings.action_retry),
                { page.chapter.pageLoader?.retryPage(page) },
                MaterialSymbols.Rounded.Refresh,
            ),
            onOpenInWebView?.let {
                ProblemAction(
                    stringResource(MR.strings.action_open_in_web_view),
                    it,
                    MaterialSymbols.Rounded.OpenInBrowser,
                )
            },
        ),
        modifier = modifier.padding(KatariTheme.spacing.xl),
    )
}
