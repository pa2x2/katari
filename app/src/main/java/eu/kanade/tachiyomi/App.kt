package eu.kanade.tachiyomi

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.util.DebugLogger
import dev.zacsweers.metro.createGraphFactory
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.crash.CrashActivity
import eu.kanade.tachiyomi.crash.GlobalExceptionHandler
import eu.kanade.tachiyomi.data.coil.BufferedSourceFetcher
import eu.kanade.tachiyomi.data.coil.CoverRequestInterceptor
import eu.kanade.tachiyomi.data.coil.EntryCoverFetcher
import eu.kanade.tachiyomi.data.coil.EntryCoverKeyer
import eu.kanade.tachiyomi.data.coil.EntryKeyer
import eu.kanade.tachiyomi.data.coil.ExtensionIconFetcher
import eu.kanade.tachiyomi.data.coil.InstalledExtensionIconKeyer
import eu.kanade.tachiyomi.data.coil.InstalledSourceIconKeyer
import eu.kanade.tachiyomi.data.coil.SourceImageFetcherFactory
import eu.kanade.tachiyomi.data.coil.SourceImageKeyer
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.GLUtil
import eu.kanade.tachiyomi.util.system.WebViewUtil
import eu.kanade.tachiyomi.util.system.animatorDurationScale
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import mihon.app.di.AppGraph
import mihon.app.di.injekt.MetroInjektRegistrar
import mihon.app.localnetwork.LocalNetworkPermissionPrompter
import mihon.core.metro.GraphProvider
import mihon.core.metro.StartupWarmup
import mihon.core.migration.Migrator
import mihon.core.migration.migrations.migrations
import mihon.entry.interactions.runtime.addEntryInteractionImageComponents
import mihon.feature.profiles.sourcepreferences.extensionPreferenceSourceId
import mihon.telemetry.TelemetryConfig
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import tachiyomi.presentation.widget.WidgetManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import kotlin.concurrent.thread
import mihon.icons.materialsymbols.R as MaterialSymbolsR

class App : Application(), DefaultLifecycleObserver, SingletonImageLoader.Factory, GraphProvider<AppGraph> {

    // Built on first use rather than in onCreate: WorkManager and content providers can reach it before onCreate runs.
    override val graph: AppGraph by lazy {
        createGraphFactory<AppGraph.Factory>().create(this).also { it.profilePreferenceOwners }
    }

    init {
        // Extensions resolve their services through Injekt, and the graph can load them before onCreate runs
        Injekt = InjektScope(MetroInjektRegistrar(application = this, graphProvider = this))
    }

    private val basePreferences: BasePreferences by lazy { graph.basePreferences }
    private val privacyPreferences: PrivacyPreferences by lazy { graph.privacyPreferences }
    private val networkPreferences: NetworkPreferences by lazy { graph.networkPreferences }

    private val disableIncognitoReceiver = DisableIncognitoReceiver()

    // Extensions open their settings by a name without a profile; SourcePreferenceFiles explains where they go. The name
    // is checked before the graph is touched, because building the graph opens preference files itself.
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        val sourceId = name?.let(::extensionPreferenceSourceId) ?: return super.getSharedPreferences(name, mode)
        return graph.sourcePreferenceFiles.forSource(sourceId)
    }

    @SuppressLint("LaunchActivityFromNotification")
    override fun onCreate() {
        super<Application>.onCreate()

        // CrashActivity has a process of its own so that it outlives the crash it reports, and that process sets up only
        // what the crash screen needs. The rest of this startup belongs to the main process: run again beside it, it
        // would restore download queues, retry merge and migration consequences and run migrations a second time, and
        // the libraries androidx.startup initialises, WorkManager among them, are not set up in any other process.
        val process = getProcessName()
        if (process != packageName) {
            onCreateCrashProcess(process)
            return
        }

        TelemetryConfig.init(applicationContext)

        GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)

        // The graph and the code after it read the default preferences straight away. Loading them takes a disk
        // read, so start it on another thread while the graph is set up; the first read then waits less or not at all
        thread(name = "PreferencesPreload") { PreferenceManager.getDefaultSharedPreferences(this) }
        // The first use of Dispatchers.Main finds its factory with a ServiceLoader, which reads the APK
        thread(name = "MainDispatcherPreload") { Dispatchers.Main }

        // Asynchronously init expensive components for a faster cold start
        ContextCompat.getMainExecutor(this).execute {
            graph.networkHelper
            graph.sourceManager
            graph.database
            graph.startupWarmups.forEach(StartupWarmup::warmUp)
        }

        setupNotificationChannels()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        val scope = graph.appScope

        LocalNetworkPermissionPrompter(this).install(scope)

        // Show notification to disable Incognito Mode when it's enabled
        basePreferences.incognitoMode.changes()
            .onEach { enabled ->
                if (enabled) {
                    disableIncognitoReceiver.register()
                    notify(
                        Notifications.ID_INCOGNITO_MODE,
                        Notifications.CHANNEL_INCOGNITO_MODE,
                    ) {
                        setContentTitle(stringResource(MR.strings.pref_incognito_mode))
                        setContentText(stringResource(MR.strings.notification_incognito_text))
                        setSmallIcon(MaterialSymbolsR.drawable.rounded_eyeglasses_2)
                        setOngoing(true)

                        val pendingIntent = PendingIntent.getBroadcast(
                            this@App,
                            0,
                            Intent(ACTION_DISABLE_INCOGNITO_MODE).setPackage(BuildConfig.APPLICATION_ID),
                            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
                        )
                        setContentIntent(pendingIntent)
                    }
                } else {
                    disableIncognitoReceiver.unregister()
                    cancelNotification(Notifications.ID_INCOGNITO_MODE)
                }
            }
            .launchIn(scope)

        privacyPreferences.analytics
            .changes()
            .onEach(TelemetryConfig::setAnalyticsEnabled)
            .launchIn(scope)

        privacyPreferences.crashlytics
            .changes()
            .onEach(TelemetryConfig::setCrashlyticsEnabled)
            .launchIn(scope)

        basePreferences.hardwareBitmapThreshold.let { preference ->
            if (!preference.isSet()) preference.set(GLUtil.DEVICE_TEXTURE_LIMIT)
        }

        basePreferences.hardwareBitmapThreshold.changes()
            .onEach { ImageUtil.hardwareBitmapThreshold = it }
            .launchIn(scope)

        setAppCompatDelegateThemeMode(graph.uiPreferences.themeMode.get())

        // Updates widget update
        WidgetManager(graph.getUpdates, graph.securityPreferences).apply { init(scope) }

        installLogcatLogger()

        initializeMigrator()
    }

    // No GlobalExceptionHandler here: it would answer a crash of the crash screen by opening the crash screen again.
    private fun onCreateCrashProcess(process: String) {
        // Dumping the crash log loads extensions, which may use WebView, and WebView refuses a data directory that
        // another process uses
        WebView.setDataDirectorySuffix(process)
        setAppCompatDelegateThemeMode(graph.uiPreferences.themeMode.get())
        installLogcatLogger()
    }

    private fun installLogcatLogger() {
        if (LogcatLogger.isInstalled) return
        val minLogPriority = when {
            networkPreferences.verboseLogging.get() -> LogPriority.VERBOSE
            BuildConfig.DEBUG -> LogPriority.DEBUG
            else -> LogPriority.INFO
        }
        LogcatLogger.install()
        LogcatLogger.loggers += AndroidLogcatLogger(minLogPriority)
    }

    private fun initializeMigrator() {
        val preferenceStore = graph.preferenceStore
        val preference = preferenceStore.getInt(Preference.appStateKey("last_version_code"), 0)
        logcat { "Migration from ${preference.get()} to ${BuildConfig.VERSION_CODE}" }
        Migrator.initialize(
            old = preference.get(),
            new = BuildConfig.VERSION_CODE,
            migrations = migrations,
            graph = graph,
            onMigrationComplete = {
                logcat { "Updating last version to ${BuildConfig.VERSION_CODE}" }
                preference.set(BuildConfig.VERSION_CODE)
            },
        )
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(this).apply {
            val callFactoryLazy = lazy { graph.networkHelper.client }
            val coverCacheLazy = lazy { graph.coverCache }
            val coverNetworkFeatureLazy = lazy { graph.entryCoverNetworkFeature }
            components {
                // NetworkFetcher.Factory
                add(OkHttpNetworkFetcherFactory(callFactoryLazy::value))
                // Fetcher.Factory
                add(BufferedSourceFetcher.Factory())
                add(EntryCoverFetcher.EntryCoverFactory(callFactoryLazy, coverCacheLazy, coverNetworkFeatureLazy))
                add(EntryCoverFetcher.EntryFactory(callFactoryLazy, coverCacheLazy, coverNetworkFeatureLazy))
                add(ExtensionIconFetcher.ExtensionFactory(context))
                add(ExtensionIconFetcher.SourceFactory(context, lazy { graph.extensionManager }))
                add(SourceImageFetcherFactory(callFactoryLazy, coverNetworkFeatureLazy))
                addEntryInteractionImageComponents(this@App)
                // Keyer
                add(EntryCoverKeyer(graph.coverCache))
                add(EntryKeyer(graph.coverCache))
                add(InstalledExtensionIconKeyer())
                add(InstalledSourceIconKeyer())
                add(SourceImageKeyer())
                // Interceptor
                add(CoverRequestInterceptor())
            }

            memoryCache(
                MemoryCache.Builder()
                    .maxSizePercent(context)
                    .build(),
            )

            crossfade((300 * this@App.animatorDurationScale).toInt())
            allowRgb565(DeviceUtil.isLowRamDevice(this@App))
            if (networkPreferences.verboseLogging.get()) logger(DebugLogger())

            // Coil spawns a new thread for every image load by default
            fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(8))
            decoderCoroutineContext(Dispatchers.IO.limitedParallelism(3))
        }
            .build()
    }

    override fun onStart(owner: LifecycleOwner) {
        SecureActivityDelegate.onApplicationStart(graph.securityPreferences)
    }

    override fun onStop(owner: LifecycleOwner) {
        SecureActivityDelegate.onApplicationStopped(graph.securityPreferences)
    }

    override fun getPackageName(): String {
        try {
            // Override the value passed as X-Requested-With in WebView requests
            val stackTrace = Thread.currentThread().stackTrace
            val isChromiumCall = stackTrace.any { trace ->
                trace.className.lowercase() in setOf("org.chromium.base.buildinfo", "org.chromium.base.apkinfo") &&
                    trace.methodName.lowercase() in setOf("getall", "getpackagename", "<init>")
            }

            if (isChromiumCall) return WebViewUtil.spoofedPackageName(applicationContext)
        } catch (_: Exception) {
        }

        return super.getPackageName()
    }

    private fun setupNotificationChannels() {
        try {
            Notifications.createChannels(this)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to modify notification channels" }
        }
        // Follows profiles as they are created, renamed, archived and deleted. Off the main thread, as opening the
        // database would slow the cold start.
        graph.appScope.launch(Dispatchers.IO) {
            graph.profileDatabase.subscribeProfiles(includeArchived = false)
                .distinctUntilChangedBy { profiles -> profiles.map { it.id to it.name } }
                .collect { profiles ->
                    try {
                        Notifications.syncLibraryUpdateChannels(this@App, profiles)
                    } catch (e: Exception) {
                        logcat(LogPriority.ERROR, e) { "Failed to update library update notification channels" }
                    }
                }
        }
    }

    private inner class DisableIncognitoReceiver : BroadcastReceiver() {
        private var registered = false

        override fun onReceive(context: Context, intent: Intent) {
            basePreferences.incognitoMode.set(false)
        }

        fun register() {
            if (!registered) {
                ContextCompat.registerReceiver(
                    this@App,
                    this,
                    IntentFilter(ACTION_DISABLE_INCOGNITO_MODE),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                registered = true
            }
        }

        fun unregister() {
            if (registered) {
                unregisterReceiver(this)
                registered = false
            }
        }
    }
}

private const val ACTION_DISABLE_INCOGNITO_MODE = "tachi.action.DISABLE_INCOGNITO_MODE"
