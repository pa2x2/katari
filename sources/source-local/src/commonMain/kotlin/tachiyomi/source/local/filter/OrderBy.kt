package tachiyomi.source.local.filter

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryFilter
import tachiyomi.i18n.*

sealed class OrderBy(localize: (StringResource) -> String, selection: Selection) : EntryFilter.Sort(
    localize(MR.strings.local_filter_order_by),
    arrayOf(localize(MR.strings.title), localize(MR.strings.date)),
    selection,
) {
    class Popular(localize: (StringResource) -> String) : OrderBy(localize, Selection(0, true))
    class Latest(localize: (StringResource) -> String) : OrderBy(localize, Selection(1, false))
}
