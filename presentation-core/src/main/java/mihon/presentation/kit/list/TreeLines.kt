package mihon.presentation.kit.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import mihon.presentation.kit.theme.KatariTheme

/**
 * The stem of an expanded item in a tree, such as a merged entry above its members: a line at [x] from [stemTop] below
 * the item's centre down to its bottom edge, where its children's [treeBranch]es carry it on.
 */
@Composable
fun Modifier.treeStem(x: Dp, stemTop: Dp): Modifier {
    val color = KatariTheme.colors.outlineVariant
    val stroke = KatariTheme.sizes.border
    return drawWithContent {
        drawContent()
        drawLine(
            color = color,
            start = Offset(x.toPx(), size.height / 2 + stemTop.toPx()),
            end = Offset(x.toPx(), size.height),
            strokeWidth = stroke.toPx(),
        )
    }
}

/**
 * A child's branch of a tree: the stem carried on at [x], stopping at this child when it is the [last], and a line
 * across to the child.
 */
@Composable
fun Modifier.treeBranch(x: Dp, last: Boolean): Modifier {
    val color = KatariTheme.colors.outlineVariant
    val stroke = KatariTheme.sizes.border
    val reach = KatariTheme.spacing.l
    return drawWithContent {
        drawContent()
        val branchX = x.toPx()
        val branchY = size.height / 2
        drawLine(
            color = color,
            start = Offset(branchX, 0f),
            end = Offset(branchX, if (last) branchY else size.height),
            strokeWidth = stroke.toPx(),
        )
        drawLine(
            color = color,
            start = Offset(branchX, branchY),
            end = Offset((x + reach).toPx(), branchY),
            strokeWidth = stroke.toPx(),
        )
    }
}
