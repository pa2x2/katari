package eu.kanade.tachiyomi.ui.reader

import android.app.Activity
import android.app.PendingIntent
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mihon.entry.interactions.reader.settings.MangaReaderSettingsProvider
import mihon.entry.interactions.reader.settings.ReaderBasePreferences
import mihon.entry.interactions.setEntryInteractionContent
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import com.google.android.material.R as MaterialR

private const val MAIN_ACTIVITY_CLASS_NAME = "eu.kanade.tachiyomi.ui.main.MainActivity"
private const val WEB_VIEW_ACTIVITY_CLASS_NAME = "eu.kanade.tachiyomi.ui.webview.WebViewActivity"

private const val WEB_VIEW_URL_KEY = "url_key"
private const val WEB_VIEW_SOURCE_KEY = "source_key"
private const val WEB_VIEW_TITLE_KEY = "title_key"

@Composable
fun ifSourcesLoaded(): Boolean {
    return remember { Injekt.get<SourceManager>().isInitialized }.collectAsState().value
}

fun AppCompatActivity.registerReaderSecureScreen() {
    val preferences = Injekt.get<ReaderBasePreferences>()
    val securityPreferences = Injekt.get<SecurityPreferences>()

    combine(
        securityPreferences.secureScreen.changes(),
        preferences.incognitoMode.changes(),
    ) { secureScreen, incognitoMode ->
        secureScreen == SecurityPreferences.SecureScreenMode.ALWAYS ||
            (secureScreen == SecurityPreferences.SecureScreenMode.INCOGNITO && incognitoMode)
    }
        .onEach { enabled ->
            if (enabled) {
                window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        .launchIn(lifecycleScope)
}

fun Context.mangaEntryIntent(entryId: Long): Intent {
    return Intent()
        .setClassName(this, MAIN_ACTIVITY_CLASS_NAME)
        .setAction(tachiyomi.core.common.Constants.SHORTCUT_MANGA)
        .putExtra(tachiyomi.core.common.Constants.ENTRY_EXTRA, entryId)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
}

fun Context.readerWebViewIntent(url: String, sourceId: Long? = null, title: String? = null): Intent {
    return Intent()
        .setClassName(this, WEB_VIEW_ACTIVITY_CLASS_NAME)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .putExtra(WEB_VIEW_URL_KEY, url)
        .putExtra(WEB_VIEW_SOURCE_KEY, sourceId)
        .putExtra(WEB_VIEW_TITLE_KEY, title)
}

fun Context.openInBrowser(uri: Uri, forceDefaultBrowser: Boolean = false) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (forceDefaultBrowser) {
                defaultBrowserPackageName()?.let(::setPackage)
            }
        }
        startActivity(intent)
    }.onFailure {
        toast(it.message)
    }
}

private fun Context.defaultBrowserPackageName(): String? {
    val browserIntent = Intent(Intent.ACTION_VIEW, "http://".toUri())
    val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(
            browserIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
    }
    return resolveInfo?.activityInfo?.packageName
}

fun Uri.toReaderShareIntent(context: Context, type: String = "image/*", message: String? = null): Intent {
    val uri = this
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        when (uri.scheme) {
            "http", "https" -> putExtra(Intent.EXTRA_TEXT, uri.toString())
            "content" -> {
                message?.let { putExtra(Intent.EXTRA_TEXT, it) }
                putExtra(Intent.EXTRA_STREAM, uri)
            }
        }
        clipData = ClipData.newRawUri(null, uri)
        setType(type)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    return Intent.createChooser(shareIntent, context.stringResource(MR.strings.action_share)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}

fun openImagePendingActivity(context: Context, uri: Uri): PendingIntent {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/*")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    return PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

fun shareImagePendingActivity(context: Context, uri: Uri): PendingIntent {
    return PendingIntent.getActivity(
        context,
        0,
        uri.toReaderShareIntent(context),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
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

fun View?.isVisibleOnScreen(): Boolean {
    if (this == null || !isShown) return false

    val actualPosition = Rect()
    getGlobalVisibleRect(actualPosition)
    val screen = Rect(
        0,
        0,
        android.content.res.Resources.getSystem().displayMetrics.widthPixels,
        android.content.res.Resources.getSystem().displayMetrics.heightPixels,
    )
    return actualPosition.intersect(screen)
}

fun Context.createReaderThemeContext(): Context {
    val preferences = Injekt.get<UiPreferences>()
    val readerPreferences = Injekt.get<MangaReaderSettingsProvider>()
    val themeMode = preferences.themeMode.get()
    val isDarkBackground = when (readerPreferences.readerTheme.get()) {
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
