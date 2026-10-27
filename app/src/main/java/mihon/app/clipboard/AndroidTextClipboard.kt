package mihon.app.clipboard

import android.content.Context
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.copyToClipboard
import mihon.core.platform.clipboard.TextClipboard

@Inject
class AndroidTextClipboard(private val context: Context) : TextClipboard {
    override fun copy(label: String, text: String) {
        context.copyToClipboard(label, text)
    }
}
