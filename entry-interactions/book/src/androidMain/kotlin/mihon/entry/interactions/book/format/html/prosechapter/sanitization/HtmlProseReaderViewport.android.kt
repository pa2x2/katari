package mihon.entry.interactions.book.format.html.prosechapter.sanitization

import android.content.res.Resources
import mihon.entry.interactions.book.format.html.prosechapter.css.CssViewport

internal actual fun screenViewport(): CssViewport? = runCatching {
    Resources.getSystem().displayMetrics.let { metrics ->
        CssViewport(metrics.widthPixels / metrics.density, metrics.heightPixels / metrics.density)
    }
}.getOrNull()
