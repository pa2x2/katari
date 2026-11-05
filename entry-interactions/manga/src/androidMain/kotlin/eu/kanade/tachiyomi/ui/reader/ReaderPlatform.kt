package eu.kanade.tachiyomi.ui.reader

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationManagerCompat
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import mihon.core.metro.metroGraph
import mihon.entry.interactions.reader.settings.ReaderBasePreferences
import mihon.entry.interactions.runtime.EntryInteractionActivityGraph
import mihon.entry.interactions.runtime.registerEntryInteractionSecureScreen
import mihon.entry.interactions.runtime.setEntryInteractionContent
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import com.google.android.material.R as MaterialR

private const val MAIN_ACTIVITY_CLASS_NAME = "eu.kanade.tachiyomi.ui.main.MainActivity"

fun AppCompatActivity.registerReaderSecureScreen(
    readerPreferences: ReaderBasePreferences,
    securityPreferences: SecurityPreferences,
) {
    registerEntryInteractionSecureScreen(readerPreferences, securityPreferences)
}

fun Context.mangaEntryIntent(entryId: Long): Intent {
    return Intent()
        .setClassName(this, MAIN_ACTIVITY_CLASS_NAME)
        .setAction(tachiyomi.core.common.Constants.SHORTCUT_MANGA)
        .putExtra(tachiyomi.core.common.Constants.ENTRY_EXTRA, entryId)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
}

fun Context.openReaderPageInWebView(url: String, sourceId: Long?) {
    metroGraph<EntryInteractionActivityGraph>().entryWebViewDestination.open(this, url, title = null, sourceId)
}

fun dismissNewChaptersNotification(context: Context, entryNotificationId: Int) {
    NotificationManagerCompat.from(context).cancel(entryNotificationId)
}

fun ComposeView.setReaderComposeContent(
    content: @Composable () -> Unit,
) {
    setEntryInteractionContent(content)
}

fun Context.isNightMode(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun Activity.hasDisplayCutout(): Boolean {
    return window.decorView.hasDisplayCutout()
}

private fun View.hasDisplayCutout(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && rootWindowInsets?.displayCutout != null
}

val Context.animatorDurationScale: Float
    get() = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

/** Whether any part of the view shows in its window. */
fun View?.isVisibleOnScreen(): Boolean {
    if (this == null || !isShown) return false
    return getGlobalVisibleRect(Rect())
}

fun Context.createReaderThemeContext(readerTheme: Int, uiPreferences: UiPreferences): Context {
    val themeMode = uiPreferences.themeMode.get()
    val isDarkBackground = when (readerTheme) {
        1, 2 -> true
        3 -> when (themeMode) {
            ThemeMode.SYSTEM -> applicationContext.isNightMode()
            else -> themeMode == ThemeMode.DARK
        }
        else -> false
    }
    val expected = if (isDarkBackground) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == expected) {
        return this
    }

    val overrideConf = Configuration()
    overrideConf.setTo(resources.configuration)
    overrideConf.uiMode = (overrideConf.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or expected

    return ContextThemeWrapper(this, MaterialR.style.Theme_Material3_DayNight_NoActionBar).apply {
        applyOverrideConfiguration(overrideConf)
    }
}

val Throwable.readerFormattedMessage: String
    get() = when (val className = this::class.simpleName) {
        null, "Exception", "IOException" -> message ?: className.orEmpty()
        else -> "$className: $message"
    }
