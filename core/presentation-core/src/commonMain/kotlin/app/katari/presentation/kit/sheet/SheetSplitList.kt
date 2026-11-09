package app.katari.presentation.kit.sheet

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.layout.RowListScope

/**
 * A sheet's list in two parts, such as a filter's types and stores beside its long list of languages. In a sheet with
 * two columns of rows the parts sit side by side, each scrolling on its own and keeping its rows to one column; in a
 * one-column sheet [start] is followed by [end] in one list. [fastScroll] gives the list holding [end] a draggable
 * thumb, for a long part such as every source by language.
 */
@Composable
fun SheetSplitList(
    start: RowListScope.() -> Unit,
    end: RowListScope.() -> Unit,
    modifier: Modifier = Modifier,
    fastScroll: Boolean = false,
) {
    if (LocalSheetColumns.current < 2) {
        RowList(modifier = modifier, fastScroll = fastScroll) {
            start()
            end()
        }
        return
    }
    CompositionLocalProvider(LocalSheetColumns provides 1) {
        Row(modifier = modifier, verticalAlignment = Alignment.Top) {
            RowList(modifier = Modifier.weight(1f), content = start)
            RowList(modifier = Modifier.weight(1f), fastScroll = fastScroll, content = end)
        }
    }
}
