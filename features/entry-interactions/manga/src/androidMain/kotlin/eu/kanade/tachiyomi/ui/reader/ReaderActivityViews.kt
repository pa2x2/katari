package eu.kanade.tachiyomi.ui.reader

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import app.katari.entry.interactions.manga.R

/**
 * The views of `reader_activity.xml`. Inflated by hand because the multiplatform Android library plugin this module
 * builds with has no view binding.
 */
internal class ReaderActivityViews private constructor(val root: FrameLayout) {
    val readerContainer: FrameLayout = root.findViewById(R.id.reader_container)
    val viewerContainer: FrameLayout = root.findViewById(R.id.viewer_container)
    val composeOverlay: ComposeView = root.findViewById(R.id.compose_overlay)

    companion object {
        fun inflate(inflater: LayoutInflater): ReaderActivityViews =
            ReaderActivityViews(inflater.inflate(R.layout.reader_activity, null, false) as FrameLayout)
    }
}
