package app.katari.android.clipboard

import android.content.Context
import app.katari.core.platform.clipboard.TextClipboard
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.copyToClipboard

@Inject
class AndroidTextClipboard(private val context: Context) : TextClipboard {
    override fun copy(label: String, text: String) {
        context.copyToClipboard(label, text)
    }
}
