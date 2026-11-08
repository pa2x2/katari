package eu.kanade.tachiyomi.ui.reader.viewer

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

/** An Android View viewer: a [ReaderViewer] drawn by a [View] that takes the activity's key and motion events. */
internal interface Viewer : ReaderViewer {
    fun getView(): View

    /** Returns whether the viewer handled the key [event]. */
    fun handleKeyEvent(event: KeyEvent): Boolean

    /** Returns whether the viewer handled the generic motion [event]. */
    fun handleGenericMotionEvent(event: MotionEvent): Boolean
}
