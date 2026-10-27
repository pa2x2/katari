package tachiyomi.source.local.filter

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryFilter
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

sealed class OrderBy(context: Context, selection: Selection) : EntryFilter.Sort(
    context.stringResource(MR.strings.local_filter_order_by),
    arrayOf(context.stringResource(MR.strings.title), context.stringResource(MR.strings.date)),
    selection,
) {
    class Popular(context: Context) : OrderBy(context, Selection(0, true))
    class Latest(context: Context) : OrderBy(context, Selection(1, false))
}
