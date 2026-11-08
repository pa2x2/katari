package app.katari.entry.interactions.book.format.html.prosechapter.sanitization

import android.content.res.Resources
import app.katari.entry.interactions.book.format.html.prosechapter.css.CssViewport

internal actual fun screenViewport(): CssViewport? = runCatching {
    Resources.getSystem().displayMetrics.let { metrics ->
        CssViewport(metrics.widthPixels / metrics.density, metrics.heightPixels / metrics.density)
    }
}.getOrNull()
