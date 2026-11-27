package app.katari.presentation.kit.button

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.katari.icons.custommaterialsymbols.CustomMaterialSymbols
import app.katari.icons.custommaterialsymbols.rounded.DownloadArrow
import app.katari.icons.custommaterialsymbols.rounded.DownloadCircle
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Close
import app.katari.icons.materialsymbols.rounded.Delete
import app.katari.icons.materialsymbols.rounded.Download
import app.katari.icons.materialsymbols.rounded.Error
import app.katari.icons.materialsymbols.roundedfilled.CheckCircle
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.menu.DropdownMenu
import app.katari.presentation.kit.menu.DropdownMenuItem
import app.katari.presentation.kit.selection.secondaryClick
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import tachiyomi.presentation.core.components.material.IconButtonTokens
import kotlin.math.roundToInt

enum class DownloadIndicatorState {
    NOT_DOWNLOADED,
    DELETING,
    QUEUE,
    DOWNLOADING,
    DOWNLOADED,
    ERROR,
}

enum class DownloadIndicatorAction {
    START,
    START_NOW,
    CANCEL,
    DELETE,
}

/** How many of a group's chapters are downloaded, out of all of them. */
@Immutable
data class DownloadedShare(val downloaded: Int, val total: Int) {
    val isPartial: Boolean get() = downloaded in 1 until total
}

/** A menu entry an indicator offers besides its own download actions. */
@Immutable
class DownloadIndicatorMenuItem(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * The download state of a chapter or group of chapters as an icon button: tap to download, follow progress, cancel or
 * delete. A long press or a right-click opens its menu, with [notDownloadedMenuItems] or [menuItems] beside its own
 * actions, such as downloading on another device. [badge] is pinned to its bottom end corner, such as the chapter's
 * translation mark.
 *
 * A group of chapters gives [downloadedShareProvider]. While some but not all of them are downloaded and none is on its
 * way, the ring around the arrow is drawn in pieces, the downloaded ones bright, so the group reads neither as
 * untouched nor as a download that stopped; its menu can then delete the downloaded ones.
 *
 * [downloadProgressProvider] gives 0–100, and 0 shows a spinner. A group's progress can be above 0 while it is queued,
 * once some of its chapters are done; the ring then keeps that share and stays still until the next one starts.
 */
@Composable
fun DownloadIndicator(
    enabled: Boolean,
    downloadStateProvider: () -> DownloadIndicatorState,
    downloadProgressProvider: () -> Int,
    startContentDescription: String,
    errorContentDescription: String,
    queuedContentDescription: String,
    downloadingContentDescription: String,
    downloadedContentDescription: String,
    partlyDownloadedContentDescription: @Composable (DownloadedShare) -> String,
    startNowText: String,
    cancelText: String,
    deleteText: String,
    deleteDownloadedText: String,
    onClick: (DownloadIndicatorAction) -> Unit,
    modifier: Modifier = Modifier,
    downloadedShareProvider: () -> DownloadedShare? = { null },
    badge: (@Composable () -> Unit)? = null,
    menuItems: List<DownloadIndicatorMenuItem> = emptyList(),
    notDownloadedMenuItems: List<DownloadIndicatorMenuItem> = emptyList(),
) {
    Box(modifier = modifier) {
        DownloadIndicatorContent(
            enabled = enabled,
            downloadState = downloadStateProvider(),
            downloadedShare = downloadedShareProvider(),
            downloadProgressProvider = downloadProgressProvider,
            startContentDescription = startContentDescription,
            errorContentDescription = errorContentDescription,
            queuedContentDescription = queuedContentDescription,
            downloadingContentDescription = downloadingContentDescription,
            downloadedContentDescription = downloadedContentDescription,
            partlyDownloadedContentDescription = partlyDownloadedContentDescription,
            startNowText = startNowText,
            cancelText = cancelText,
            deleteText = deleteText,
            deleteDownloadedText = deleteDownloadedText,
            menuItems = menuItems,
            notDownloadedMenuItems = notDownloadedMenuItems,
            onClick = onClick,
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(BadgeInset),
            ) {
                badge()
            }
        }
    }
}

@Composable
private fun DownloadIndicatorContent(
    enabled: Boolean,
    downloadState: DownloadIndicatorState,
    downloadedShare: DownloadedShare?,
    downloadProgressProvider: () -> Int,
    startContentDescription: String,
    errorContentDescription: String,
    queuedContentDescription: String,
    downloadingContentDescription: String,
    downloadedContentDescription: String,
    partlyDownloadedContentDescription: @Composable (DownloadedShare) -> String,
    startNowText: String,
    cancelText: String,
    deleteText: String,
    deleteDownloadedText: String,
    menuItems: List<DownloadIndicatorMenuItem>,
    notDownloadedMenuItems: List<DownloadIndicatorMenuItem>,
    onClick: (DownloadIndicatorAction) -> Unit,
) {
    when (downloadState) {
        DownloadIndicatorState.NOT_DOWNLOADED if downloadedShare?.isPartial == true -> PartlyDownloadedIndicator(
            enabled = enabled,
            downloadedShare = downloadedShare,
            contentDescription = partlyDownloadedContentDescription(downloadedShare),
            startNowText = startNowText,
            deleteDownloadedText = deleteDownloadedText,
            menuItems = notDownloadedMenuItems,
            onClick = onClick,
        )
        DownloadIndicatorState.NOT_DOWNLOADED -> NotDownloadedIndicator(
            enabled = enabled,
            startContentDescription = startContentDescription,
            startNowText = startNowText,
            menuItems = notDownloadedMenuItems,
            onClick = onClick,
        )
        DownloadIndicatorState.DELETING -> DeletingIndicator()
        DownloadIndicatorState.QUEUE,
        DownloadIndicatorState.DOWNLOADING,
        -> DownloadingIndicator(
            enabled = enabled,
            downloadState = downloadState,
            downloadProgressProvider = downloadProgressProvider,
            queuedContentDescription = queuedContentDescription,
            downloadingContentDescription = downloadingContentDescription,
            startNowText = startNowText,
            cancelText = cancelText,
            menuItems = menuItems,
            onClick = onClick,
        )
        DownloadIndicatorState.DOWNLOADED -> DownloadedIndicator(
            enabled = enabled,
            deleteText = deleteText,
            downloadedContentDescription = downloadedContentDescription,
            menuItems = menuItems,
            onClick = onClick,
        )
        DownloadIndicatorState.ERROR -> ErrorIndicator(
            enabled = enabled,
            errorContentDescription = errorContentDescription,
            onClick = onClick,
        )
    }
}

@Composable
private fun DeletingIndicator(
    modifier: Modifier = Modifier,
) {
    val strokeColor = KatariTheme.colors.onSurfaceVariant
    val eraseTransition = rememberInfiniteTransition(label = "deleting-erase")
    val eraseProgress by eraseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "deleting-erase-progress",
    )

    Box(
        modifier = modifier.size(IconButtonTokens.StateLayerSize),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = IndicatorModifier,
            color = strokeColor.copy(alpha = DELETING_RING_ALPHA),
            strokeWidth = IndicatorStrokeWidth,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round,
        )
        Icon(
            imageVector = MaterialSymbols.RoundedFilled.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .size(IndicatorSize)
                .alpha((1f - eraseProgress).coerceIn(DELETING_RING_ALPHA, 1f))
                .drawWithContent {
                    clipRect(right = size.width * (1f - eraseProgress)) {
                        this@drawWithContent.drawContent()
                    }
                },
            tone = ContentTone.Secondary,
        )
    }
}

@Composable
private fun NotDownloadedIndicator(
    enabled: Boolean,
    startContentDescription: String,
    startNowText: String,
    menuItems: List<DownloadIndicatorMenuItem>,
    onClick: (DownloadIndicatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(IconButtonTokens.StateLayerSize)
            .commonClickable(
                enabled = enabled,
                hapticFeedback = LocalHapticFeedback.current,
                // Without other choices a long press starts the download right away, as it always did.
                onLongClick = {
                    if (menuItems.isEmpty()) onClick(DownloadIndicatorAction.START_NOW) else isMenuExpanded = true
                },
                onClick = { onClick(DownloadIndicatorAction.START) },
            )
            .secondaryClick(enabled) { isMenuExpanded = true },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = CustomMaterialSymbols.Rounded.DownloadCircle,
            contentDescription = startContentDescription,
            modifier = Modifier.size(IndicatorSize),
            tone = ContentTone.Secondary,
        )
        DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
            DropdownMenuItem(
                label = startNowText,
                onClick = {
                    onClick(DownloadIndicatorAction.START_NOW)
                    isMenuExpanded = false
                },
                icon = MaterialSymbols.Rounded.Download,
            )
            MenuItems(menuItems, onDismiss = { isMenuExpanded = false })
        }
    }
}

@Composable
private fun PartlyDownloadedIndicator(
    enabled: Boolean,
    downloadedShare: DownloadedShare,
    contentDescription: String,
    startNowText: String,
    deleteDownloadedText: String,
    menuItems: List<DownloadIndicatorMenuItem>,
    onClick: (DownloadIndicatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(IconButtonTokens.StateLayerSize)
            .commonClickable(
                enabled = enabled,
                hapticFeedback = LocalHapticFeedback.current,
                onLongClick = { isMenuExpanded = true },
                onClick = { onClick(DownloadIndicatorAction.START) },
            )
            .secondaryClick(enabled) { isMenuExpanded = true },
        contentAlignment = Alignment.Center,
    ) {
        val color = KatariTheme.colors.onSurfaceVariant
        Canvas(modifier = Modifier.size(IndicatorSize)) {
            drawDownloadedPieces(downloadedShare, color)
        }
        Icon(
            imageVector = CustomMaterialSymbols.Rounded.DownloadArrow,
            contentDescription = contentDescription,
            modifier = ArrowModifier,
            tone = ContentTone.Secondary,
        )
        DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
            DropdownMenuItem(
                label = startNowText,
                onClick = {
                    onClick(DownloadIndicatorAction.START_NOW)
                    isMenuExpanded = false
                },
                icon = MaterialSymbols.Rounded.Download,
            )
            MenuItems(menuItems, onDismiss = { isMenuExpanded = false })
            DropdownMenuItem(
                label = deleteDownloadedText,
                onClick = {
                    onClick(DownloadIndicatorAction.DELETE)
                    isMenuExpanded = false
                },
                icon = MaterialSymbols.Rounded.Delete,
            )
        }
    }
}

/**
 * The ring of the download icon cut into pieces, one per chapter up to [MAX_DOWNLOADED_PIECES]; past that each piece
 * stands for a share of the group. A piece is lit while anything is downloaded and one stays dim until all are, so the
 * ring never looks untouched or complete when it isn't. The pieces have gaps and don't move, unlike a running download's
 * continuous wavy ring.
 */
private fun DrawScope.drawDownloadedPieces(share: DownloadedShare, color: Color) {
    val pieces = minOf(share.total, MAX_DOWNLOADED_PIECES)
    val lit = if (share.total <= MAX_DOWNLOADED_PIECES) {
        share.downloaded
    } else {
        (share.downloaded * pieces.toFloat() / share.total).roundToInt().coerceIn(1, pieces - 1)
    }
    // The ring's centre line and width in the download_circle icon, which spans 960 units
    val radius = size.minDimension * RING_RADIUS
    val litStroke = size.minDimension * RING_WIDTH
    val dimStroke = litStroke * DIM_PIECE_WIDTH_RATIO
    val step = 360f / pieces
    val gap = minOf(MAX_PIECE_GAP_DEGREES, step * PIECE_GAP_RATIO)
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2, radius * 2)
    repeat(pieces) { index ->
        val isLit = index < lit
        drawArc(
            color = if (isLit) color else color.copy(alpha = DIM_PIECE_ALPHA),
            startAngle = -90f + index * step + gap / 2,
            sweepAngle = step - gap,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = if (isLit) litStroke else dimStroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun MenuItems(items: List<DownloadIndicatorMenuItem>, onDismiss: () -> Unit) {
    items.forEach { item ->
        DropdownMenuItem(
            label = item.text,
            onClick = {
                item.onClick()
                onDismiss()
            },
            icon = item.icon,
        )
    }
}

@Composable
private fun DownloadingIndicator(
    enabled: Boolean,
    downloadState: DownloadIndicatorState,
    downloadProgressProvider: () -> Int,
    queuedContentDescription: String,
    downloadingContentDescription: String,
    startNowText: String,
    cancelText: String,
    menuItems: List<DownloadIndicatorMenuItem>,
    onClick: (DownloadIndicatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(IconButtonTokens.StateLayerSize)
            .commonClickable(
                enabled = enabled,
                hapticFeedback = LocalHapticFeedback.current,
                onLongClick = { onClick(DownloadIndicatorAction.CANCEL) },
                onClick = { isMenuExpanded = true },
            )
            .secondaryClick(enabled) { isMenuExpanded = true },
        contentAlignment = Alignment.Center,
    ) {
        val color = KatariTheme.colors.onSurfaceVariant
        val trackColor = color.copy(alpha = TRACK_ALPHA)
        val stroke = Stroke(width = with(LocalDensity.current) { IndicatorStrokeWidth.toPx() }, cap = StrokeCap.Round)
        val downloadProgress = downloadProgressProvider()
        val indeterminate = downloadProgress == 0
        if (indeterminate) {
            CircularWavyProgressIndicator(
                modifier = WavyIndicatorModifier,
                color = color,
                trackColor = Color.Transparent,
                stroke = stroke,
                trackStroke = stroke,
                // Only an active download waves, so a queued one reads as waiting
                amplitude = if (downloadState == DownloadIndicatorState.DOWNLOADING) INDICATOR_AMPLITUDE else 0f,
                wavelength = IndicatorWavelength,
            )
        } else {
            val animatedProgress by animateFloatAsState(
                targetValue = downloadProgress / 100f,
                animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "progress",
            )
            CircularWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = WavyIndicatorModifier,
                color = color,
                trackColor = trackColor,
                stroke = stroke,
                trackStroke = stroke,
                amplitude = {
                    if (downloadState == DownloadIndicatorState.DOWNLOADING) {
                        WavyProgressIndicatorDefaults.indicatorAmplitude(it) * INDICATOR_AMPLITUDE
                    } else {
                        0f
                    }
                },
                wavelength = IndicatorWavelength,
            )
        }
        DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
            DropdownMenuItem(
                label = startNowText,
                onClick = {
                    onClick(DownloadIndicatorAction.START_NOW)
                    isMenuExpanded = false
                },
                icon = MaterialSymbols.Rounded.Download,
            )
            DropdownMenuItem(
                label = cancelText,
                onClick = {
                    onClick(DownloadIndicatorAction.CANCEL)
                    isMenuExpanded = false
                },
                icon = MaterialSymbols.Rounded.Close,
            )
            MenuItems(menuItems, onDismiss = { isMenuExpanded = false })
        }
        Icon(
            imageVector = CustomMaterialSymbols.Rounded.DownloadArrow,
            contentDescription = if (downloadState == DownloadIndicatorState.QUEUE) {
                queuedContentDescription
            } else {
                downloadingContentDescription
            },
            modifier = ArrowModifier,
            tone = ContentTone.Secondary,
        )
    }
}

@Composable
private fun DownloadedIndicator(
    enabled: Boolean,
    deleteText: String,
    downloadedContentDescription: String,
    menuItems: List<DownloadIndicatorMenuItem>,
    onClick: (DownloadIndicatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(IconButtonTokens.StateLayerSize)
            .commonClickable(
                enabled = enabled,
                hapticFeedback = LocalHapticFeedback.current,
                onLongClick = { isMenuExpanded = true },
                onClick = { isMenuExpanded = true },
            )
            .secondaryClick(enabled) { isMenuExpanded = true },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MaterialSymbols.RoundedFilled.CheckCircle,
            contentDescription = downloadedContentDescription,
            modifier = Modifier.size(IndicatorSize),
            tone = ContentTone.Secondary,
        )
        DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
            MenuItems(menuItems, onDismiss = { isMenuExpanded = false })
            DropdownMenuItem(
                label = deleteText,
                onClick = {
                    onClick(DownloadIndicatorAction.DELETE)
                    isMenuExpanded = false
                },
                icon = MaterialSymbols.Rounded.Delete,
            )
        }
    }
}

@Composable
private fun ErrorIndicator(
    enabled: Boolean,
    errorContentDescription: String,
    onClick: (DownloadIndicatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(IconButtonTokens.StateLayerSize)
            .commonClickable(
                enabled = enabled,
                hapticFeedback = LocalHapticFeedback.current,
                onLongClick = { onClick(DownloadIndicatorAction.START) },
                onClick = { onClick(DownloadIndicatorAction.START) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Error,
            contentDescription = errorContentDescription,
            modifier = Modifier.size(IndicatorSize),
            tone = ContentTone.Error,
        )
    }
}

private fun Modifier.commonClickable(
    enabled: Boolean,
    hapticFeedback: HapticFeedback,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) = this.combinedClickable(
    enabled = enabled,
    onLongClick = {
        onLongClick()
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    },
    onClick = onClick,
    role = Role.Button,
    interactionSource = null,
    indication = ripple(
        bounded = false,
        radius = IconButtonTokens.StateLayerSize / 2,
    ),
)

private val IndicatorSize = 26.dp
private val BadgeInset = 4.dp
private val IndicatorPadding = 2.dp
private val IndicatorStrokeWidth = 2.dp
private val WavyIndicatorPadding = 1.dp
private val IndicatorWavelength = 7.dp
private const val INDICATOR_AMPLITUDE = 0.6f
private const val TRACK_ALPHA = 0.25f
private const val DELETING_RING_ALPHA = 0.18f
private const val MAX_DOWNLOADED_PIECES = 8
private const val RING_RADIUS = 365f / 960f
private const val RING_WIDTH = 70f / 960f
private const val DIM_PIECE_WIDTH_RATIO = 0.6f
private const val DIM_PIECE_ALPHA = 0.45f
private const val MAX_PIECE_GAP_DEGREES = 26f
private const val PIECE_GAP_RATIO = 0.3f

private val IndicatorModifier = Modifier
    .size(IndicatorSize)
    .padding(IndicatorPadding)

private val WavyIndicatorModifier = Modifier
    .size(IndicatorSize)
    .padding(WavyIndicatorPadding)

private val ArrowModifier = Modifier
    .size(IndicatorSize)
