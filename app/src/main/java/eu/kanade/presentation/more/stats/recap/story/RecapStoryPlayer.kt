package eu.kanade.presentation.more.stats.recap.story

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import eu.kanade.presentation.more.stats.recap.components.LocalRecapReveal
import eu.kanade.presentation.more.stats.recap.components.LocalRecapTitleTargets
import eu.kanade.presentation.more.stats.recap.components.RecapTitleTargets
import eu.kanade.presentation.more.stats.recap.page.RecapPage
import eu.kanade.presentation.more.stats.recap.palette.RecapPalette
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapStory
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapTitle
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

/**
 * Plays a recap story: each page enters, stays a while and moves on by itself; tapping the left third goes back,
 * elsewhere forward; holding pauses, and holding a title offers to hide it; swiping down closes. The story stops on
 * its last page, the summary card.
 *
 * @param paletteOf the colours of a page.
 * @param title how the period reads above the story and on its pages, such as "2026".
 * @param footer the line at the foot of every page.
 * @param onShare receives the current page as a 1080 by 1920 image.
 */
@Composable
internal fun RecapStoryPlayer(
    story: StatisticsRecapStory,
    paletteOf: (StatisticsRecapPage) -> RecapPalette,
    title: String,
    footer: String,
    includeNsfw: Boolean,
    hiddenTitles: List<StatisticsRecapTitle>,
    onIncludeNsfwChange: (Boolean) -> Unit,
    onHide: (Long) -> Unit,
    onShowAgain: (Long) -> Unit,
    onShare: suspend (ImageBitmap) -> Unit,
    onClose: () -> Unit,
) {
    val pages = story.pages
    var index by rememberSaveable(story.period) { mutableIntStateOf(0) }
    val current = index.coerceIn(0, pages.lastIndex)
    val page = pages[current]
    val palette = paletteOf(page)
    val background by animateColorAsState(palette.background, label = "recapBackground")
    val backgroundEnd by animateColorAsState(palette.backgroundEnd, label = "recapBackgroundEnd")

    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val targets = remember { RecapTitleTargets() }
    val reveal = remember { Animatable(0f) }
    var progress by remember { mutableFloatStateOf(0f) }
    var replays by remember { mutableIntStateOf(0) }
    var holding by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var hiddenDialogOpen by remember { mutableStateOf(false) }
    var hideOffer by remember { mutableStateOf<Pair<Long, Offset>?>(null) }
    var sharing by remember { mutableStateOf(false) }
    var pageArea by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val paused by rememberUpdatedState(
        holding || menuOpen || hiddenDialogOpen || hideOffer != null || sharing ||
            !lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
    )

    LaunchedEffect(current, replays, pages.size) {
        progress = 0f
        reveal.snapTo(0f)
        launch { reveal.animateTo(1f, tween(REVEAL_MILLIS, easing = LinearEasing)) }
        var elapsed = 0L
        var last = withFrameMillis { it }
        while (elapsed < PAGE_MILLIS) {
            val now = withFrameMillis { it }
            if (!paused) elapsed += now - last
            last = now
            progress = elapsed / PAGE_MILLIS.toFloat()
        }
        if (current < pages.lastIndex) index = current + 1
    }

    // The tap handler outlives compositions, so these read the page index when called rather than capturing it.
    fun previous() {
        if (index > 0) index-- else replays++
    }

    fun next() {
        if (index < pages.lastIndex) index++
    }

    fun share() {
        scope.launch {
            sharing = true
            try {
                reveal.snapTo(1f)
                // Let the finished entrance draw into the layer before reading it back.
                withFrameMillis {}
                withFrameMillis {}
                onShare(layer.toImageBitmap())
            } finally {
                sharing = false
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(background, backgroundEnd))),
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            if (pages.size > 1) {
                RecapProgressBars(
                    count = pages.size,
                    index = current,
                    progress = progress,
                    color = palette.ink,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            RecapTopBar(
                title = title,
                palette = palette,
                onMore = { menuOpen = true },
                onClose = onClose,
                moreMenu = {
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(MR.strings.statistics_recap_include_nsfw)) },
                            trailingIcon = { Checkbox(checked = includeNsfw, onCheckedChange = null) },
                            onClick = { onIncludeNsfwChange(!includeNsfw) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(MR.strings.statistics_recap_hidden_titles)) },
                            onClick = {
                                menuOpen = false
                                hiddenDialogOpen = true
                            },
                        )
                    }
                },
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .onGloballyPositioned { pageArea = it }
                    .pointerInput(onClose) {
                        var dragged = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dragged = 0f },
                            onDragEnd = { if (dragged > CLOSE_DRAG.toPx()) onClose() },
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                dragged += amount
                            },
                        )
                    }
                    .pointerInput(pages.size) {
                        detectTapGestures(
                            onPress = {
                                holding = true
                                tryAwaitRelease()
                                holding = false
                            },
                            onLongPress = { offset ->
                                val root = pageArea?.localToRoot(offset) ?: return@detectTapGestures
                                targets.entryAt(root)?.let { hideOffer = it to offset }
                            },
                            onTap = { offset -> if (offset.x < size.width / 3f) previous() else next() },
                        )
                    },
            ) {
                CompositionLocalProvider(
                    LocalRecapReveal provides reveal.asState(),
                    LocalRecapTitleTargets provides targets,
                ) {
                    RecapPageFit(Modifier.fillMaxSize()) {
                        RecapPage(
                            page = page,
                            palette = palette,
                            periodTitle = title,
                            footer = footer,
                            modifier = Modifier.drawWithContent {
                                layer.record { this@drawWithContent.drawContent() }
                                drawLayer(layer)
                            },
                        )
                    }
                }
                HideTitleMenu(
                    offer = hideOffer,
                    onHide = { entryId ->
                        hideOffer = null
                        onHide(entryId)
                    },
                    onDismiss = { hideOffer = null },
                )
            }
            RecapBottomBar(
                isLastPage = current == pages.lastIndex,
                canReplay = pages.size > 1,
                palette = palette,
                onShare = ::share,
                onReplay = {
                    index = 0
                    replays++
                },
            )
        }
    }

    if (hiddenDialogOpen) {
        RecapHiddenTitlesDialog(
            titles = hiddenTitles,
            onShowAgain = onShowAgain,
            onDismiss = { hiddenDialogOpen = false },
        )
    }
}

/** The menu offered where a title was held. */
@Composable
private fun HideTitleMenu(offer: Pair<Long, Offset>?, onHide: (Long) -> Unit, onDismiss: () -> Unit) {
    val position = offer?.second ?: Offset.Zero
    Box(Modifier.offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }) {
        DropdownMenu(expanded = offer != null, onDismissRequest = onDismiss) {
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.statistics_recap_hide_title)) },
                onClick = { offer?.let { onHide(it.first) } },
            )
        }
    }
}

private const val REVEAL_MILLIS = 1_100
private const val PAGE_MILLIS = 8_000L
private val CLOSE_DRAG = 120.dp
