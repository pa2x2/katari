package eu.kanade.tachiyomi.ui.reader.viewer

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import mihon.entry.interactions.manga.R

/**
 * The views of `reader_error.xml`, which a page holder shows when its page fails to load. Inflated by hand because the
 * multiplatform Android library plugin this module builds with has no view binding.
 */
internal class ReaderErrorViews private constructor(val root: LinearLayout) {
    val errorMessage: TextView = root.findViewById(R.id.error_message)
    val actionRetry: ReaderButton = root.findViewById(R.id.action_retry)
    val actionOpenInWebView: ReaderButton = root.findViewById(R.id.action_open_in_web_view)

    companion object {
        /** Inflates the layout and adds it to the end of [parent]. */
        fun inflateInto(inflater: LayoutInflater, parent: ViewGroup): ReaderErrorViews {
            val root = inflater.inflate(R.layout.reader_error, parent, false)
            parent.addView(root)
            return ReaderErrorViews(root as LinearLayout)
        }
    }
}
