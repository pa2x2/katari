package mihon.entry.interactions.source

import android.content.Context
import android.content.Intent
import android.net.Uri
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

private const val WEB_VIEW_ACTIVITY_CLASS_NAME = "eu.kanade.tachiyomi.ui.webview.WebViewActivity"
private const val WEB_VIEW_URL_KEY = "url_key"
private const val WEB_VIEW_SOURCE_KEY = "source_key"
private const val WEB_VIEW_TITLE_KEY = "title_key"

fun Context.launchEntryChildWebViewAction(
    action: EntryChildWebViewAction,
    resolution: EntryChildWebViewResolution.Available,
    title: String?,
): Result<Unit> = runCatching {
    val intent = when (action) {
        EntryChildWebViewAction.OPEN_IN_WEB_VIEW -> Intent()
            .setClassName(this, WEB_VIEW_ACTIVITY_CLASS_NAME)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(WEB_VIEW_URL_KEY, resolution.url)
            .putExtra(WEB_VIEW_SOURCE_KEY, resolution.sourceId)
            .putExtra(WEB_VIEW_TITLE_KEY, title)
        EntryChildWebViewAction.OPEN_IN_BROWSER -> Intent(Intent.ACTION_VIEW, Uri.parse(resolution.url))
        EntryChildWebViewAction.SHARE -> Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, resolution.url)
                type = "text/plain"
            },
            stringResource(MR.strings.action_share),
        )
    }
    startActivity(intent)
}
