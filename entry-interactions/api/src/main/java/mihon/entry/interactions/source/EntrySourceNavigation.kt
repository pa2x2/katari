package mihon.entry.interactions.source

import android.content.Context
import android.content.Intent
import tachiyomi.core.common.Constants

private const val MAIN_ACTIVITY_CLASS_NAME = "eu.kanade.tachiyomi.ui.main.MainActivity"

/** The main activity's screens a viewer opens to fix what keeps it from showing an entry's content. */
object EntrySourceNavigation {
    const val ACTION_OPEN_SOURCE_SETTINGS = "mihon.entry.interactions.action.OPEN_SOURCE_SETTINGS"
    const val EXTRA_SOURCE_ID = "mihon.entry.interactions.extra.SOURCE_ID"
}

fun Context.openExtensions() {
    startActivity(mainActivityIntent(Constants.SHORTCUT_EXTENSIONS))
}

fun Context.openEntry(entryId: Long) {
    startActivity(mainActivityIntent(Constants.SHORTCUT_ANIME).putExtra(Constants.ENTRY_EXTRA, entryId))
}

fun Context.openSourceSettings(sourceId: Long) {
    startActivity(
        mainActivityIntent(EntrySourceNavigation.ACTION_OPEN_SOURCE_SETTINGS)
            .putExtra(EntrySourceNavigation.EXTRA_SOURCE_ID, sourceId),
    )
}

// Clearing the viewer above the main activity, as the app's own shortcuts do.
private fun Context.mainActivityIntent(action: String): Intent = Intent(action)
    .setClassName(packageName, MAIN_ACTIVITY_CLASS_NAME)
    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
