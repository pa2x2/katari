package mihon.presentation.kit.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Info
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * Where a row sits in a group of rows drawn as one card. Lazy lists compose every row as its own item, so the card is
 * drawn slice by slice: the outer rows round their outer corners and every row after the first draws the divider
 * above it.
 */
enum class RowGroupPosition {
    Alone,
    First,
    Middle,
    Last,
    ;

    companion object {
        fun of(index: Int, count: Int): RowGroupPosition = when {
            count == 1 -> Alone
            index == 0 -> First
            index == count - 1 -> Last
            else -> Middle
        }

        /**
         * Positions for a list that alternates runs of rows with other items, such as the date headers between them:
         * each run is one card, and the other items get null.
         */
        fun <T> ofRuns(items: List<T>, isRow: (T) -> Boolean): List<RowGroupPosition?> {
            val positions = arrayOfNulls<RowGroupPosition>(items.size)
            var start = 0
            while (start < items.size) {
                if (!isRow(items[start])) {
                    start++
                    continue
                }
                var end = start
                while (end < items.size && isRow(items[end])) end++
                for (index in start..<end) positions[index] = of(index - start, end - start)
                start = end
            }
            return positions.asList()
        }
    }
}

/**
 * One row's slice of a row group card. The rows inside keep their own padding against the card's edge rather than the
 * screen's. [selected] fills the slice, such as the open category in a two-pane settings list. A row that
 * [continuesAbove] belongs to the row above it, such as one update of an expanded group, so no divider separates them.
 * A [lifted] row is held out of the card, such as while it is dragged to a new place: it becomes a raised card of its own.
 */
@Composable
fun RowGroupItem(
    position: RowGroupPosition,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    continuesAbove: Boolean = false,
    lifted: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (!LocalRowGroupFramed.current) {
        Box(modifier) { content() }
        return
    }
    val large = MaterialTheme.shapes.large
    val square = CornerSize(0)
    val shape = when (if (lifted) RowGroupPosition.Alone else position) {
        RowGroupPosition.Alone -> large
        RowGroupPosition.First -> large.copy(bottomStart = square, bottomEnd = square)
        RowGroupPosition.Middle -> KatariTheme.shapes.none
        RowGroupPosition.Last -> large.copy(topStart = square, topEnd = square)
    }
    val container = LocalRowGroupContainer.current
    Surface(
        modifier = modifier.padding(horizontal = KatariTheme.spacing.m),
        container = when {
            selected -> SurfaceContainer.Selected
            lifted -> container.raised()
            else -> container
        },
        shape = shape,
        shadow = lifted,
    ) {
        Column {
            val dividerAbove = position == RowGroupPosition.Middle || position == RowGroupPosition.Last
            if (!lifted && !continuesAbove && dividerAbove) {
                HorizontalDivider()
            }
            CompositionLocalProvider(
                LocalRowInset provides KatariTheme.spacing.l,
                LocalInRowGroup provides true,
                content = content,
            )
        }
    }
}

/** The header above a row group card, lined up with the text inside the card rather than the card's edge. */
@Composable
fun RowGroupHeader(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: ContentTone = ContentTone.Inherit,
    trailing: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    CompositionLocalProvider(LocalRowInset provides rowGroupTextInset()) {
        SectionHeader(
            text = text,
            modifier = modifier,
            icon = icon,
            tone = tone,
            trailing = trailing,
            actions = actions,
        )
    }
}

/**
 * A note set between row group cards, lined up with the text inside them: how the rows below are used, a card's
 * stand-in when it has nothing to list, or with [showIcon] an explanation under a group. [tone] marks a note that reports
 * a problem, such as a choice that is no longer available.
 */
@Composable
fun RowGroupNote(
    text: String,
    modifier: Modifier = Modifier,
    showIcon: Boolean = false,
    tone: ContentTone = ContentTone.Inherit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = rowGroupTextInset(), vertical = KatariTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
    ) {
        if (showIcon) Icon(MaterialSymbols.Rounded.Info, contentDescription = null, tone = ContentTone.Secondary)
        Text(text, role = TextRole.Supporting, tone = tone)
    }
}

/** The space between one row group card and the next group below it. */
@Composable
fun RowGroupGap() {
    Spacer(Modifier.height(KatariTheme.spacing.m))
}

/**
 * [rows] as one row group card in a plain column, for a short group composed in one piece rather than as a lazy list's
 * items.
 */
@Composable
fun RowGroup(rows: List<@Composable () -> Unit>, modifier: Modifier = Modifier) {
    Column(modifier) {
        rows.forEachIndexed { index, row ->
            RowGroupItem(position = RowGroupPosition.of(index, rows.size), content = row)
        }
    }
}

/**
 * The rows [content] adds as one row group card in a plain column, for a group whose rows depend on conditions, such
 * as a sheet's filters that only show for some entry types. Conditions are decided when the group is built, so read
 * the state they depend on before it; state a row only displays belongs inside that row. Nothing is drawn when no row
 * is added.
 */
@Composable
fun RowGroup(modifier: Modifier = Modifier, content: RowGroupScope.() -> Unit) {
    val rows = RowGroupScope().apply(content).rows
    if (rows.isEmpty()) return
    Column(modifier) {
        rows.forEachIndexed { index, row ->
            key(row.key ?: index) {
                RowGroupItem(position = RowGroupPosition.of(index, rows.size), content = row.content)
            }
        }
    }
}

class RowGroupScope internal constructor() {
    internal class GroupRow(val key: Any?, val content: @Composable () -> Unit)

    internal val rows = mutableListOf<GroupRow>()

    /** Adds a row to the card. [key] keeps a row's state with it while rows above it come and go. */
    fun row(key: Any? = null, content: @Composable () -> Unit) {
        rows += GroupRow(key, content)
    }
}

// The card's side margin, where content set between cards lines up with their edges.
@Composable
internal fun rowGroupEdgeInset() = if (LocalRowGroupFramed.current) KatariTheme.spacing.m else LocalRowInset.current

// The card's side margin plus the row inset inside it; unframed rows keep the inset around them.
@Composable
private fun rowGroupTextInset() = if (LocalRowGroupFramed.current) {
    KatariTheme.spacing.m + KatariTheme.spacing.l
} else {
    LocalRowInset.current
}

/**
 * The fill of row group cards. A sheet is already raised a step above the screen, so it sets a lighter fill for the
 * cards on it to stand out from the sheet as they do from the screen.
 */
internal val LocalRowGroupContainer = staticCompositionLocalOf { SurfaceContainer.Default }

private fun SurfaceContainer.raised(): SurfaceContainer = when (this) {
    SurfaceContainer.Default -> SurfaceContainer.High
    else -> SurfaceContainer.Highest
}

/**
 * Whether row groups draw as cards. A dialog is already a card around its content, so groups inside it, such as a
 * shared editor that is also shown on a sheet, are drawn as plain rows.
 */
internal val LocalRowGroupFramed = staticCompositionLocalOf { true }

/** Rows inside a row group card set their icon on a tonal tile, as the card already separates them from the screen. */
internal val LocalInRowGroup = staticCompositionLocalOf { false }

@Composable
internal fun RowIconTile(icon: ImageVector) {
    Surface(container = SurfaceContainer.SecondaryContainer, shape = KatariTheme.shapes.small) {
        Box(modifier = Modifier.size(IconTileSize), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null)
        }
    }
}

private val IconTileSize = 40.dp

/**
 * [items] as one row group card, each row a [RowGroupItem] slice in its place that animates as the list changes.
 * [selected] fills a row's slice, such as the open category in a two-pane settings list.
 */
fun <T> LazyListScope.rowGroupItems(
    items: List<T>,
    key: ((T) -> Any)? = null,
    contentType: (T) -> Any? = { null },
    selected: (T) -> Boolean = { false },
    itemContent: @Composable (T) -> Unit,
) {
    itemsIndexed(
        items = items,
        key = key?.let { { _, item -> it(item) } },
        contentType = { _, item -> contentType(item) },
    ) { index, item ->
        RowGroupItem(
            position = RowGroupPosition.of(index, items.size),
            modifier = Modifier.animateItem(),
            selected = selected(item),
        ) {
            itemContent(item)
        }
    }
}
