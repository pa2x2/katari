package mihon.entry.interactions.reader.settings

import android.content.pm.ActivityInfo

/** The `requestedOrientation` a reader activity applies for this orientation. */
val ReaderOrientation.activityOrientation: Int
    get() = when (this) {
        ReaderOrientation.DEFAULT, ReaderOrientation.FREE -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        ReaderOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        ReaderOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        ReaderOrientation.LOCKED_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ReaderOrientation.LOCKED_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        ReaderOrientation.REVERSE_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
    }
