package app.katari.android.share

import android.content.Intent
import app.katari.core.platform.PlatformContext
import app.katari.core.platform.share.TextSharing
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/** Offers the text as plain text through the system share sheet, and toasts why when the sheet can't open. */
@Inject
class AndroidTextSharing : TextSharing {
    override val isAvailable: Boolean = true

    override fun share(context: PlatformContext, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            context.startActivity(Intent.createChooser(intent, context.stringResource(MR.strings.action_share)))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }
}
