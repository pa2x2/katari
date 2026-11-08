package app.katari.entry.interactions.download

import app.katari.core.platform.PlatformContext
import tachiyomi.i18n.platform.stringResource

fun EntryDownloadMessage.resolve(context: PlatformContext): String = when (this) {
    is EntryDownloadMessage.Text -> value
    is EntryDownloadMessage.Resource -> context.stringResource(resource, *args.toTypedArray())
}
