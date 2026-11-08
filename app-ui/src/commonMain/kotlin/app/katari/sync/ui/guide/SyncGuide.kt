package app.katari.sync.ui.guide

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import mihon.presentation.kit.list.RowGroupAction
import tachiyomi.core.common.Constants
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The website's sync pages, as paths under [Constants.URL_DOCS]. Their sections' ids are kept once published. */
object SyncGuide {
    const val SYNC = "sync/"
    const val SERVER = "sync-server/"
}

/** A problem only whoever runs the server can fix, and the guide section that says how. */
enum class ServerFix(val page: String) {
    UPDATE("${SyncGuide.SERVER}#update"),
    SET_UP_ADMIN("${SyncGuide.SERVER}#admin"),
}

/** Opens a page of the guide, such as [SyncGuide.SERVER], in the browser. */
@Composable
fun rememberOpenSyncGuide(): (page: String) -> Unit {
    val uriHandler = LocalUriHandler.current
    return { page -> uriHandler.openUri(Constants.URL_DOCS + page) }
}

/** A link to the guide's [page] between row group cards. */
@Composable
fun SyncGuideAction(label: String, page: String) {
    val open = rememberOpenSyncGuide()
    RowGroupAction(label = label, onClick = { open(page) }, icon = MaterialSymbols.AutoMirroredRounded.OpenInNew)
}

/** The link to how to [fix] the server, under the note that names the problem. */
@Composable
fun ServerFixAction(fix: ServerFix) {
    SyncGuideAction(serverFixLabel(fix), fix.page)
}

@Composable
fun serverFixLabel(fix: ServerFix): String = stringResource(
    when (fix) {
        ServerFix.UPDATE -> MR.strings.sync_guide_update_server
        ServerFix.SET_UP_ADMIN -> MR.strings.sync_guide_admin_account
    },
)
