package app.katari.entry.interactions.runtime

import android.app.Activity

fun interface EntryInteractionActivityTheme {
    /** Applies the host application's current theme before the activity creates any UI. */
    fun apply(activity: Activity)
}
