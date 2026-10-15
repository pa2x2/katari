package mihon.presentation.kit.button

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mihon.icons.custommaterialsymbols.CustomMaterialSymbols
import mihon.icons.custommaterialsymbols.rounded.DownloadArrow
import mihon.icons.custommaterialsymbols.rounded.DownloadCircle
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Error
import mihon.icons.materialsymbols.roundedfilled.CheckCircle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.presentation.core.components.material.IconButtonTokens

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

/** A menu entry an indicator offers besides its own download actions. */
@Immutable
class DownloadIndicatorMenuItem(
    val text: String,
    val onClick: () -> Unit,
)

/**
 * The download state of a chapter or group of chapters as an icon button: tap to download, follow progress, cancel or
 * delete. [badge] is pinned to its bottom end corner, such as the chapter's translation mark.
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
    startNowText: String,
    cancelText: String,
    deleteText: String,
    onClick: (DownloadIndicatorAction) -> Unit,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
    menuItems: List<DownloadIndicatorMenuItem> = emptyList(),
    notDownloadedMenuItems: List<DownloadIndicatorMenuItem> = emptyList(),
) {
    Box(modifier = modifier) {
        DownloadIndicatorContent(
            enabled = enabled,
            downloadState = downloadStateProvider(),
            downloadProgressProvider = downloadProgressProvider,
            startContentDescription = startContentDescription,
            errorContentDescription = errorContentDescription,
            queuedContentDescription = queuedContentDescription,
            downloadingContentDescription = downloadingContentDescription,
            downloadedContentDescription = downloadedContentDescription,
            startNowText = startNowText,
            cancelText = cancelText,
            deleteText = deleteText,
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
    downloadProgressProvider: () -> Int,
    startContentDescription: String,
    errorContentDescription: String,
    queuedContentDescription: String,
    downloadingContentDescription: String,
    downloadedContentDescription: String,
    startNowText: String,
    cancelText: String,
    deleteText: String,
    menuItems: List<DownloadIndicatorMenuItem>,
    notDownloadedMenuItems: List<DownloadIndicatorMenuItem>,
    onClick: (DownloadIndicatorAction) -> Unit,
) {
    when (downloadState) {
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
            ),
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
            )
            MenuItems(menuItems, onDismiss = { isMenuExpanded = false })
        }
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
            ),
        contentAlignment = Alignment.Center,
    ) {
        val color = KatariTheme.colors.onSurfaceVariant
        val trackColor = color.copy(alpha = TRACK_ALPHA)
        val stroke = Stroke(width = with(LocalDensity.current) { IndicatorStrokeWidth.toPx() }, cap = StrokeCap.Round)
        val downloadProgress = downloadProgressProvider()
        val indeterminate = downloadState == DownloadIndicatorState.QUEUE ||
            (downloadState == DownloadIndicatorState.DOWNLOADING && downloadProgress == 0)
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
                amplitude = { WavyProgressIndicatorDefaults.indicatorAmplitude(it) * INDICATOR_AMPLITUDE },
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
            )
            DropdownMenuItem(
                label = cancelText,
                onClick = {
                    onClick(DownloadIndicatorAction.CANCEL)
                    isMenuExpanded = false
                },
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
            ),
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

private val IndicatorModifier = Modifier
    .size(IndicatorSize)
    .padding(IndicatorPadding)

private val WavyIndicatorModifier = Modifier
    .size(IndicatorSize)
    .padding(WavyIndicatorPadding)

private val ArrowModifier = Modifier
    .size(IndicatorSize)
