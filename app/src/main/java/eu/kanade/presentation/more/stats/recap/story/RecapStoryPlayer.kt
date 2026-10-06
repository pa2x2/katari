package eu.kanade.presentation.more.stats.recap.story

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import eu.kanade.presentation.more.stats.recap.components.LocalRecapTitleTargets
import eu.kanade.presentation.more.stats.recap.components.RecapTitleTargets
import eu.kanade.presentation.more.stats.recap.motion.LocalRecapClock
import eu.kanade.presentation.more.stats.recap.motion.RecapClock
import eu.kanade.presentation.more.stats.recap.page.RecapPage
import eu.kanade.presentation.more.stats.recap.page.drawRecapBackdrop
import eu.kanade.presentation.more.stats.recap.palette.RecapPalette
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapStory
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapTitle
import eu.kanade.tachiyomi.util.system.animatorDurationScale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

/**
 * Plays a recap story: each page enters, stays a while and moves on by itself, except the year's opening page, which
 * waits for a tap. Tapping the left third goes back, elsewhere forward; holding pauses, and holding a title offers to
 * hide it; swiping down closes. The story stops on its last page, the summary card.
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
    val shownPalette = animateRecapPalette(palette)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val targets = remember { RecapTitleTargets() }
    val clock = remember { RecapClock(still = context.animatorDurationScale == 0f) }
    val waitsForTap = page is StatisticsRecapPage.Opening
    var holding by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var hiddenDialogOpen by remember { mutableStateOf(false) }
    var hideOffer by remember { mutableStateOf<Pair<Long, Offset>?>(null) }
    var sharing by remember { mutableStateOf(false) }
    var pageArea by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Both are written on every move, though the objects stay the same, so the backdrop follows the page.
    var screenCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null, neverEqualPolicy()) }
    var pageCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null, neverEqualPolicy()) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val paused by rememberUpdatedState(
        holding || menuOpen || hiddenDialogOpen || hideOffer != null || sharing ||
            !lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
    )

    LaunchedEffect(clock) {
        var last = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            if (!paused) clock.advance(now - last)
            last = now
        }
    }

    // The page restarts with the index rather than in an effect afterwards, so a new page's first frame doesn't show
    // it already entered.
    fun goTo(target: Int) {
        index = target
        clock.restartPage()
    }

    LaunchedEffect(current, waitsForTap) {
        if (waitsForTap) return@LaunchedEffect
        snapshotFlow { clock.pageMillis >= PAGE_MILLIS }.first { it }
        if (current < pages.lastIndex) goTo(current + 1)
    }

    // The tap handler outlives compositions, so these read the page index when called rather than capturing it.
    fun previous() {
        if (index > 0) goTo(index - 1)
    }

    fun next() {
        if (index < pages.lastIndex) goTo(index + 1)
    }

    fun share() {
        scope.launch {
            sharing = true
            try {
                clock.skipEntrance()
                // Let the finished entrance draw into the layer, which records only while sharing, before reading it.
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
            .onGloballyPositioned { screenCoordinates = it }
            .drawBehind {
                val screen = screenCoordinates
                val shown = pageCoordinates
                val bounds = if (screen?.isAttached == true && shown?.isAttached == true) {
                    screen.localBoundingBoxOf(shown)
                } else {
                    Rect(Offset.Zero, size)
                }
                drawRecapBackdrop(shownPalette, clock.storyMillis, bounds)
            },
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            if (pages.size > 1) {
                RecapProgressBars(
                    count = pages.size,
                    index = current,
                    progress = { if (waitsForTap) 0f else clock.pageMillis / PAGE_MILLIS.toFloat() },
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
                    LocalRecapClock provides clock,
                    LocalRecapTitleTargets provides targets,
                ) {
                    RecapPageFit(Modifier.fillMaxSize()) {
                        RecapPage(
                            page = page,
                            palette = palette,
                            periodTitle = title,
                            footer = footer,
                            modifier = Modifier
                                .onGloballyPositioned { pageCoordinates = it }
                                .drawWithContent {
                                    if (sharing) {
                                        layer.record {
                                            drawRecapBackdrop(palette, clock.storyMillis, Rect(Offset.Zero, size))
                                            this@drawWithContent.drawContent()
                                        }
                                    }
                                    drawContent()
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
                onReplay = { goTo(0) },
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

/** [palette] with each colour easing into the next page's, so pages change colour without a cut. */
@Composable
private fun animateRecapPalette(palette: RecapPalette): RecapPalette = RecapPalette(
    background = animateColorAsState(palette.background, label = "recapBackground").value,
    backgroundEnd = animateColorAsState(palette.backgroundEnd, label = "recapBackgroundEnd").value,
    ink = animateColorAsState(palette.ink, label = "recapInk").value,
    accent = animateColorAsState(palette.accent, label = "recapAccent").value,
    muted = animateColorAsState(palette.muted, label = "recapMuted").value,
    faint = animateColorAsState(palette.faint, label = "recapFaint").value,
)

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

private const val PAGE_MILLIS = 8_000L
private val CLOSE_DRAG = 120.dp
