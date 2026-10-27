package mihon.entry.interactions.download

import android.content.Context
import tachiyomi.i18n.context.stringResource

fun EntryDownloadMessage.resolve(context: Context): String = when (this) {
    is EntryDownloadMessage.Text -> value
    is EntryDownloadMessage.Resource -> context.stringResource(resource, *args.toTypedArray())
}
