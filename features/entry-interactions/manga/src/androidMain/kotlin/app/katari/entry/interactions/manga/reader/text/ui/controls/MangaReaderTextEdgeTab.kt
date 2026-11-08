package app.katari.entry.interactions.manga.reader.text.ui.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.entry.interactions.manga.reader.text.session.MangaReaderTextProgress
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Translate
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.NotificationBadge
import app.katari.presentation.kit.progress.LoadingIndicator
import app.katari.presentation.kit.surface.EdgeTab
import app.katari.presentation.kit.surface.TabEdge
import app.katari.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Translate mode's controls tucked into a screen edge: a small tab that shows whether pages are being processed or
 * need attention, and brings the toolbar back when tapped.
 */
@Composable
internal fun MangaReaderTextEdgeTab(
    edge: MangaReaderTextControlsEdge,
    progress: MangaReaderTextProgress,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandLabel = stringResource(MR.strings.reader_text_controls_expand)
    EdgeTab(
        edge = when (edge) {
            MangaReaderTextControlsEdge.Left -> TabEdge.Start
            MangaReaderTextControlsEdge.Right -> TabEdge.End
        },
        onClick = onExpand,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.size(KatariTheme.sizes.iconLarge), contentAlignment = Alignment.Center) {
            if (progress.isWorking) {
                LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.iconLarge))
            }
            Icon(
                imageVector = MaterialSymbols.Rounded.Translate,
                contentDescription = expandLabel,
                modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            )
        }
        if (progress.needsAttention) {
            NotificationBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = KatariTheme.spacing.xxs),
            )
        }
    }
}

internal val MangaReaderTextProgress.isWorking: Boolean
    get() = this == MangaReaderTextProgress.Recognizing || this == MangaReaderTextProgress.Translating

private val MangaReaderTextProgress.needsAttention: Boolean
    get() = this is MangaReaderTextProgress.Blocked ||
        this is MangaReaderTextProgress.Failed ||
        this is MangaReaderTextProgress.TranslationUnavailable
