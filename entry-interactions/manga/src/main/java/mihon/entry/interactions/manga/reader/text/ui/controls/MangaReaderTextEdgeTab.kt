package mihon.entry.interactions.manga.reader.text.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import mihon.entry.interactions.manga.reader.text.session.MangaReaderTextProgress
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
    Surface(
        onClick = onExpand,
        modifier = modifier
            .size(width = TAB_WIDTH, height = TAB_HEIGHT)
            .alpha(TAB_ALPHA),
        shape = when (edge) {
            MangaReaderTextControlsEdge.Left -> RoundedCornerShape(topEnd = TAB_RADIUS, bottomEnd = TAB_RADIUS)
            MangaReaderTextControlsEdge.Right -> RoundedCornerShape(topStart = TAB_RADIUS, bottomStart = TAB_RADIUS)
        },
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (progress.isWorking) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            }
            Icon(
                imageVector = Icons.Outlined.Translate,
                contentDescription = expandLabel,
                modifier = Modifier.size(20.dp),
            )
            if (progress.needsAttention) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape),
                )
            }
        }
    }
}

internal val MangaReaderTextProgress.isWorking: Boolean
    get() = this == MangaReaderTextProgress.Recognizing || this == MangaReaderTextProgress.Translating

private val MangaReaderTextProgress.needsAttention: Boolean
    get() = this is MangaReaderTextProgress.Blocked ||
        this is MangaReaderTextProgress.Failed ||
        this is MangaReaderTextProgress.TranslationUnavailable

private val TAB_WIDTH = 40.dp
private val TAB_HEIGHT = 56.dp
private val TAB_RADIUS = 28.dp
private const val TAB_ALPHA = 0.85f
