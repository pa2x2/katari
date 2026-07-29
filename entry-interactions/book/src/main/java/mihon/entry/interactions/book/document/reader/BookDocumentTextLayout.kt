package mihon.entry.interactions.book.document.reader

import android.os.Build
import android.text.StaticLayout
import android.widget.TextView

/**
 * Keeps off-screen pagination measurement and the rendered [TextView] on the same platform text-layout policy.
 */
internal fun StaticLayout.Builder.applyBookDocumentTextLayoutPolicy(): StaticLayout.Builder = apply {
    setIncludePad(false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        setUseLineSpacingFromFallbacks(true)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        setUseBoundsForWidth(true)
    }
}

internal fun TextView.applyBookDocumentTextLayoutPolicy() {
    includeFontPadding = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        setFallbackLineSpacing(true)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        setUseBoundsForWidth(true)
    }
}
