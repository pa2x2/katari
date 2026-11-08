package app.katari.entry.interactions.source

import android.content.Context
import android.content.Intent
import app.katari.core.metro.metroGraph
import app.katari.entry.interactions.runtime.EntryInteractionActivityGraph
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/** Fails when the web view or the share sheet can't be opened; a link no browser opens is told by the link opener. */
fun Context.launchEntryChildWebViewAction(
    action: EntryChildWebViewAction,
    resolution: EntryChildWebViewResolution.Available,
    title: String?,
): Result<Unit> = runCatching {
    val graph = metroGraph<EntryInteractionActivityGraph>()
    when (action) {
        EntryChildWebViewAction.OPEN_IN_WEB_VIEW ->
            graph.entryWebViewDestination.open(this, resolution.url, title, resolution.sourceId)
        EntryChildWebViewAction.OPEN_IN_BROWSER -> graph.linkOpener.open(this, resolution.url)
        EntryChildWebViewAction.SHARE -> startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, resolution.url)
                    type = "text/plain"
                },
                stringResource(MR.strings.action_share),
            ),
        )
    }
}
