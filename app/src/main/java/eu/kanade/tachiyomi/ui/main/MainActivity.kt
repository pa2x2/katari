package eu.kanade.tachiyomi.ui.main

import android.animation.ValueAnimator
import android.app.SearchManager
import android.app.assist.AssistContent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.animation.doOnEnd
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.presentation.components.DownloadedOnlyBannerContainer
import eu.kanade.presentation.components.IncognitoModeBannerContainer
import eu.kanade.presentation.components.IndexingBannerContainer
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.base.delegate.requestAppUnlock
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.browse.source.preferences.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.deeplink.DeepLinkScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.library.update.LibraryUpdateSettingsNavigation
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportNavigation
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportScreen
import eu.kanade.tachiyomi.ui.main.link.AppLink
import eu.kanade.tachiyomi.ui.main.link.openAppLink
import eu.kanade.tachiyomi.ui.security.BiometricAuthentication.authenticate
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.stats.recap.StatisticsRecapNavigation
import eu.kanade.tachiyomi.ui.stats.recap.StatisticsRecapScreen
import eu.kanade.tachiyomi.ui.translator.TranslatorScreen
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.isBenchmarkBuildType
import eu.kanade.tachiyomi.util.system.isNavigationBarNeedsScrim
import eu.kanade.tachiyomi.util.view.setComposeContent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.app.di.appGraph
import mihon.app.ui.host.HomeHost
import mihon.app.ui.host.HomeLaunchState
import mihon.entry.interactions.source.EntrySourceNavigation
import mihon.entry.viewer.settings.navigation.ViewerSettingsNavigation
import mihon.feature.appupdate.ui.AppUpdatePrompt
import mihon.feature.migration.review.SourceMigrationReviewScreen
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.work.SourceMigrationNotifier
import mihon.feature.profiles.core.Profile
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.background
import mihon.presentation.kit.surface.isLight
import mihon.translation.api.host.TranslationSettingsNavigation
import tachiyomi.core.common.Constants
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import tachiyomi.presentation.core.i18n.stringResource

class MainActivity : BaseActivity() {

    private val uiPreferences by lazy { appGraph.uiPreferences }
    private val securityPreferences by lazy { appGraph.securityPreferences }
    private val profileManager by lazy { appGraph.profileManager }
    private val profilePendingIntent by lazy { appGraph.profilePendingIntent }
    private val sourceMigrationSessionStore by lazy { appGraph.sourceMigrationSessionStore }

    private val entryMergeNavigationFeature by lazy { appGraph.entryMergeNavigationFeature }

    // To be checked by splash screen. If true then splash screen will be removed.
    private var ready = false

    private var homeLaunch = HomeLaunchState()

    private var navigator: Navigator? = null
    private var newIntentHandling: Job? = null
    private var allowAppUnlockPrompt = true

    init {
        registerSecureActivity(this)
    }

    override fun shouldRequestAppUnlock(activity: AppCompatActivity): Boolean {
        return allowAppUnlockPrompt
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val isFreshLaunch = savedInstanceState == null
        // Prevent splash screen showing up on configuration changes.
        // Install it before resolving any lazy dependencies that can open the database.
        val splashScreen = if (isFreshLaunch) installSplashScreen() else null
        val splashStartTime = SystemClock.uptimeMillis()

        val savedStartupCompleted = savedInstanceState?.getBoolean(STATE_STARTUP_COMPLETED) == true
        val savedAllowAppUnlockPrompt = savedInstanceState
            ?.takeIf { it.containsKey(STATE_ALLOW_APP_UNLOCK_PROMPT) }
            ?.getBoolean(STATE_ALLOW_APP_UNLOCK_PROMPT)
        val hasUnresolvedStartupDecision = !savedStartupCompleted && savedAllowAppUnlockPrompt == null
        val startupRestorationDecision = resolveStartupRestorationDecision(
            startupCompleted = savedStartupCompleted,
            restoredAllowAppUnlockPrompt = savedAllowAppUnlockPrompt,
            // Suppress lifecycle app-lock routing until the asynchronous gate knows
            // whether the profile picker must be shown first.
            shouldShowPickerOnLaunch = hasUnresolvedStartupDecision,
        )
        homeLaunch = HomeLaunchState(isCompleted = savedStartupCompleted)
        allowAppUnlockPrompt = startupRestorationDecision.allowAppUnlockPrompt

        super.onCreate(savedInstanceState)

        // Do not let the launcher create a new activity http://stackoverflow.com/questions/16283079
        if (!isTaskRoot) {
            finish()
            return
        }

        setComposeContent {
            HomeHost(
                launchState = homeLaunch,
                launchScope = lifecycleScope,
                onReady = { ready = true },
                profileGate = { banners, home -> ProfileStartupGate(startupRestorationDecision, banners, home) },
                onNavigator = ::onHomeNavigator,
                overlay = {
                    // Draw navigation bar scrim when needed
                    if (remember { isNavigationBarNeedsScrim() }) {
                        Spacer(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                                .background(SurfaceContainer.Chrome),
                        )
                    }
                },
                navigatorContent = {
                    if (!isBenchmarkBuildType) AppUpdatePrompt()
                },
                offersOnboarding = !isBenchmarkBuildType,
            )
        }

        splashScreen?.setKeepOnScreenCondition {
            val elapsed = SystemClock.uptimeMillis() - splashStartTime
            !ready && elapsed <= SPLASH_MAX_DURATION
        }
        setSplashScreenExitAnimation(splashScreen)
    }

    /**
     * The profile picker or unlock at start, as [startupRestorationDecision] resumes it, then [home] once a profile is
     * open; the status bar follows [banners].
     */
    @Composable
    private fun ProfileStartupGate(
        startupRestorationDecision: StartupRestorationDecision,
        banners: AppStateBannerState,
        home: @Composable () -> Unit,
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val activity = context as MainActivity

        val visibleProfiles by profileManager.visibleProfiles.collectAsState()
        val activeProfile by profileManager.activeProfile.collectAsState()
        var startupProfiles by remember { mutableStateOf(emptyList<Profile>()) }
        val gateProfiles = visibleProfiles.ifEmpty { startupProfiles }
        var startupGateState by rememberSaveable {
            mutableStateOf<ProfileStartupGateState>(
                if (startupRestorationDecision.shouldResumeStartup) {
                    ProfileStartupGateState.Loading
                } else {
                    ProfileStartupGateState.Ready
                },
            )
        }
        var pendingAuthProfileId by rememberSaveable { mutableStateOf<Long?>(null) }
        var pendingSelectedProfileId by rememberSaveable { mutableStateOf<Long?>(null) }
        val pendingAuthProfile = remember(pendingAuthProfileId, activeProfile, gateProfiles) {
            when {
                pendingAuthProfileId == null -> null
                activeProfile?.id == pendingAuthProfileId -> activeProfile
                else -> gateProfiles.firstOrNull { it.id == pendingAuthProfileId }
            }
        }

        suspend fun completeStartupProfileSelection(profileId: Long) {
            allowAppUnlockPrompt = true
            profileManager.setActiveProfile(profileId)
            setAppCompatDelegateThemeMode(uiPreferences.themeMode.get())
            activity.intent = Intent(activity.intent).apply {
                action = Intent.ACTION_MAIN
                data = null
                replaceExtras(Bundle())
            }
        }

        val isSystemInDarkTheme = isSystemInDarkTheme()
        val statusBarContainer = when {
            banners.indexing -> IndexingBannerContainer
            banners.downloadedOnly -> DownloadedOnlyBannerContainer
            banners.incognito -> IncognitoModeBannerContainer
            else -> SurfaceContainer.Surface
        }
        val lightStatusBar = statusBarContainer.isLight()
        LaunchedEffect(isSystemInDarkTheme, lightStatusBar) {
            applyMainSystemBarStyle(
                lightStatusBar = lightStatusBar,
                darkTheme = isSystemInDarkTheme,
            )
        }

        LaunchedEffect(startupRestorationDecision.shouldResumeStartup, startupGateState) {
            if (!startupRestorationDecision.shouldResumeStartup ||
                startupGateState != ProfileStartupGateState.Loading
            ) {
                return@LaunchedEffect
            }

            profilePendingIntent.store(intent)
            val startupSnapshot = profileManager.loadStartupSnapshot()
            val initialProfile = startupSnapshot.initialProfile
            startupProfiles = startupSnapshot.visibleProfiles
            setAppCompatDelegateThemeMode(uiPreferences.themeMode.get())

            val requiresProfileUnlock =
                initialProfile?.let { profileManager.profileRequiresUnlock(it.id) } == true
            val shouldSkipProfileAuth = shouldSkipStartupProfileAuth()
            val startupDecision = resolveInitialStartupGateDecision(
                shouldShowPicker = startupSnapshot.shouldShowPicker,
                initialProfile = initialProfile,
                requiresProfileUnlock = requiresProfileUnlock,
                shouldSkipProfileAuth = shouldSkipProfileAuth,
            )
            allowAppUnlockPrompt = startupDecision.allowAppUnlockPrompt
            pendingAuthProfileId = startupDecision.pendingAuthProfile?.id
            startupGateState = startupDecision.state
            if (
                !startupSnapshot.shouldShowPicker &&
                initialProfile != null &&
                requiresProfileUnlock &&
                shouldSkipProfileAuth
            ) {
                requestAppUnlock(activity)
            }
        }

        LaunchedEffect(startupGateState, gateProfiles, activeProfile?.id) {
            if (
                startupGateState == ProfileStartupGateState.Picker &&
                gateProfiles.size == 1
            ) {
                val profile = activeProfile ?: gateProfiles.firstOrNull()
                val requiresProfileUnlock =
                    profile?.let { profileManager.profileRequiresUnlock(it.id) } == true
                val shouldSkipProfileAuth = shouldSkipStartupProfileAuth()
                val startupDecision = resolvePickerCollapseStartupGateDecision(
                    profile = profile,
                    requiresProfileUnlock = requiresProfileUnlock,
                    shouldSkipProfileAuth = shouldSkipProfileAuth,
                )
                allowAppUnlockPrompt = startupDecision.allowAppUnlockPrompt
                pendingAuthProfileId = startupDecision.pendingAuthProfile?.id
                if (startupDecision.state == ProfileStartupGateState.Ready) {
                    setAppCompatDelegateThemeMode(uiPreferences.themeMode.get())
                }
                startupGateState = startupDecision.state
                if (profile != null && requiresProfileUnlock && shouldSkipProfileAuth) {
                    requestAppUnlock(activity)
                }
            }
        }

        LaunchedEffect(startupGateState, pendingAuthProfileId, pendingAuthProfile?.id) {
            if (startupGateState != ProfileStartupGateState.Authenticating) return@LaunchedEffect

            if (pendingAuthProfileId == null) {
                startupGateState = ProfileStartupGateState.Ready
                return@LaunchedEffect
            }
            val profile = pendingAuthProfile ?: return@LaunchedEffect

            if (authenticateProfile(profile)) {
                profileManager.markProfileAuthenticated(profile.id)
                SecureActivityDelegate.unlock()
                val selectedProfileId = pendingSelectedProfileId
                if (selectedProfileId != null) {
                    completeStartupProfileSelection(selectedProfileId)
                } else {
                    allowAppUnlockPrompt = true
                }
                pendingAuthProfileId = null
                pendingSelectedProfileId = null
                startupGateState = ProfileStartupGateState.Ready
            } else if (pendingSelectedProfileId != null) {
                pendingAuthProfileId = null
                pendingSelectedProfileId = null
                startupGateState = ProfileStartupGateState.Picker
            } else {
                finishAffinity()
            }
        }

        LaunchedEffect(startupGateState) {
            if (
                startupGateState == ProfileStartupGateState.Picker ||
                startupGateState == ProfileStartupGateState.Authenticating
            ) {
                ready = true
            }
        }

        if (startupGateState == ProfileStartupGateState.Ready) {
            home()
        } else {
            ProfileGateContent(
                state = startupGateState,
                profiles = gateProfiles,
                activeProfileId = activeProfile?.id,
                isLocked = { profileManager.profileRequiresUnlock(it.id) },
                authProfileName = pendingAuthProfile?.name,
                onProfileSelected = { profile ->
                    if (profileManager.profileRequiresUnlock(profile.id)) {
                        pendingSelectedProfileId = profile.id
                        pendingAuthProfileId = profile.id
                        startupGateState = ProfileStartupGateState.Authenticating
                    } else {
                        scope.launch {
                            completeStartupProfileSelection(profile.id)
                            pendingAuthProfileId = null
                            pendingSelectedProfileId = null
                            startupGateState = ProfileStartupGateState.Ready
                        }
                    }
                },
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_STARTUP_COMPLETED, homeLaunch.isCompleted)
        outState.putBoolean(STATE_ALLOW_APP_UNLOCK_PROMPT, allowAppUnlockPrompt)
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        when (val screen = navigator?.lastItem) {
            is AssistContentScreen -> {
                screen.onProvideAssistUrl()?.let { outContent.webUri = it.toUri() }
            }
        }
    }

    override fun reportFullyDrawn() {
        try {
            super.reportFullyDrawn()
        } catch (e: SecurityException) {
            // Some Samsung Android 9 to 11 builds deny the platform's fully drawn broadcast
            // and the report only feeds startup metrics, so it shouldn't crash the app
            logcat(LogPriority.WARN, e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // An activity recreated after process death gets the intent before its first frame has composed a
        // navigator, so the intent waits for the home launch instead of being dropped.
        profilePendingIntent.store(intent)
        val navigator = navigator ?: return
        if (homeLaunch.isCompleted) {
            newIntentHandling?.cancel()
            newIntentHandling = lifecycleScope.launch {
                profilePendingIntent.consume()?.let { handleIntentAction(it, navigator) }
            }
        }
    }

    /**
     * Sets custom splash screen exit animation on devices prior to Android 12.
     *
     * When custom animation is used, status and navigation bar color will be set to transparent and will be restored
     * after the animation is finished.
     */
    private fun setSplashScreenExitAnimation(splashScreen: SplashScreen?) {
        val root = findViewById<View>(android.R.id.content)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && splashScreen != null) {
            window.clearSystemBarColors()

            splashScreen.setOnExitAnimationListener { splashProvider ->
                // For some reason the SplashScreen applies (incorrect) Y translation to the iconView
                splashProvider.iconView.translationY = 0F

                val activityAnim = ValueAnimator.ofFloat(1F, 0F).apply {
                    interpolator = LinearOutSlowInInterpolator()
                    duration = SPLASH_EXIT_ANIM_DURATION
                    addUpdateListener { va ->
                        val value = va.animatedValue as Float
                        root.translationY = value * 16.dpToPx
                    }
                }

                val splashAnim = ValueAnimator.ofFloat(1F, 0F).apply {
                    interpolator = FastOutSlowInInterpolator()
                    duration = SPLASH_EXIT_ANIM_DURATION
                    addUpdateListener { va ->
                        val value = va.animatedValue as Float
                        splashProvider.view.alpha = value
                    }
                    doOnEnd {
                        splashProvider.remove()
                    }
                }

                activityAnim.start()
                splashAnim.start()
            }
        }
    }

    private suspend fun handleIntentAction(intent: Intent, navigator: Navigator): Boolean {
        if (intent.action == SourceMigrationNotifier.ACTION_OPEN_SOURCE_MIGRATION_SESSION) {
            val sessionId = intent.getStringExtra(SourceMigrationNotifier.EXTRA_SOURCE_MIGRATION_SESSION_ID)
                ?.takeIf(String::isNotBlank)
                ?.let(::SourceMigrationSessionId)
                ?: return false
            val session = sourceMigrationSessionStore.get(sessionId) ?: return false
            if (session.stage.isTerminal) return false
            intent.putExtra(Constants.PROFILE_EXTRA, session.profileId)
        }
        when (routeIntentToProfile(intent)) {
            IntentProfileRouting.READY -> Unit
            IntentProfileRouting.SWITCHED -> return true
            IntentProfileRouting.REJECTED -> return false
        }
        val notificationId = intent.getIntExtra("notificationId", -1)
        if (notificationId > -1) {
            NotificationReceiver.dismissNotification(
                applicationContext,
                notificationId,
                intent.getIntExtra("groupId", 0),
            )
        }
        if (NotificationReceiver.dispatchProfileNotificationIntent(this, intent)) return true

        val tabToOpen = when (intent.action) {
            Constants.SHORTCUT_LIBRARY,
            Constants.SHORTCUT_MANGA,
            Constants.SHORTCUT_ANIME,
            Constants.SHORTCUT_UPDATES,
            Constants.SHORTCUT_HISTORY,
            Constants.SHORTCUT_SOURCES,
            Constants.SHORTCUT_EXTENSIONS,
            Constants.SHORTCUT_DOWNLOADS,
            -> {
                val tab = resolveShortcutTab(
                    action = intent.action,
                    entryIdToOpen = intent.extras
                        ?.takeIf { it.containsKey(Constants.ENTRY_EXTRA) }
                        ?.getLong(Constants.ENTRY_EXTRA),
                ) ?: return false
                if (intent.action != Constants.SHORTCUT_LIBRARY) {
                    navigator.popUntilRoot()
                }
                tab
            }
            Intent.ACTION_APPLICATION_PREFERENCES -> {
                navigator.push(SettingsScreen())
                null
            }
            LibraryUpdateReportNavigation.ACTION_OPEN_REPORT -> {
                navigator.popUntilRoot()
                navigator.push(LibraryUpdateReportScreen())
                null
            }
            StatisticsRecapNavigation.ACTION_OPEN_RECAP -> {
                navigator.popUntilRoot()
                navigator.push(StatisticsRecapScreen(StatisticsRecapNavigation.periodOf(intent) ?: return false))
                null
            }
            EntrySourceNavigation.ACTION_OPEN_SOURCE_SETTINGS -> {
                val sourceId = intent.getLongExtra(EntrySourceNavigation.EXTRA_SOURCE_ID, -1L)
                    .takeIf { it != -1L }
                    ?: return false
                navigator.push(SourcePreferencesScreen(sourceId))
                null
            }
            TranslationSettingsNavigation.ACTION_OPEN_SETTINGS -> {
                navigator.push(SettingsScreen(SettingsScreen.Destination.Translation))
                null
            }
            LibraryUpdateSettingsNavigation.ACTION_OPEN_SETTINGS -> {
                navigator.popUntilRoot()
                navigator.push(SettingsScreen(SettingsScreen.Destination.LibraryUpdates))
                null
            }
            SourceMigrationNotifier.ACTION_OPEN_SOURCE_MIGRATION_SESSION -> {
                val sessionId = intent.getStringExtra(SourceMigrationNotifier.EXTRA_SOURCE_MIGRATION_SESSION_ID)
                    ?.takeIf(String::isNotBlank)
                    ?.let(::SourceMigrationSessionId)
                    ?: return false
                navigator.popUntilRoot()
                navigator.push(SourceMigrationReviewScreen(sessionId))
                null
            }
            ViewerSettingsNavigation.ACTION_OPEN_SETTINGS -> {
                navigator.push(
                    SettingsScreen(
                        destination = SettingsScreen.Destination.Readers,
                        viewerSettingsSurfaceId = intent.getStringExtra(
                            ViewerSettingsNavigation.EXTRA_SURFACE_ID,
                        ),
                        viewerSettingsHighlight = intent.getStringExtra(ViewerSettingsNavigation.EXTRA_HIGHLIGHT),
                    ),
                )
                null
            }
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?.takeIf(String::isNotBlank)
                    ?.let { navigator.push(TranslatorScreen(it)) }
                null
            }
            Intent.ACTION_SEARCH, "com.google.android.gms.actions.SEARCH_ACTION" -> {
                // If the intent match the "standard" Android search intent
                // or the Google-specific search intent (triggered by saying or typing "search *query* on *Tachiyomi*" in Google Search/Google Assistant)

                // Get the search query provided in extras, and if not null, perform a global search with it.
                val query = intent.getStringExtra(SearchManager.QUERY)
                if (!query.isNullOrEmpty()) {
                    navigator.popUntilRoot()
                    navigator.push(DeepLinkScreen(query))
                }
                null
            }
            INTENT_SEARCH -> {
                val query = intent.getStringExtra(INTENT_SEARCH_QUERY)
                if (!query.isNullOrEmpty()) {
                    val filter = intent.getStringExtra(INTENT_SEARCH_FILTER)
                    navigator.popUntilRoot()
                    navigator.push(GlobalSearchScreen(query, filter))
                }
                null
            }
            Intent.ACTION_VIEW -> {
                intent.data?.let { AppLink.of(AndroidUriLinkParts(it)) }?.let(navigator::openAppLink)
                null
            }
            else -> return false
        }

        if (tabToOpen != null) {
            lifecycleScope.launch { HomeScreen.openTab(tabToOpen) }
        }

        return true
    }

    private suspend fun routeIntentToProfile(intent: Intent): IntentProfileRouting {
        val explicitProfileId = intent.getLongExtra(Constants.PROFILE_EXTRA, -1L)
        val profileId = if (explicitProfileId > -1L) {
            explicitProfileId
        } else {
            val legacyNotificationEntryId = intent.extras
                ?.takeIf { intent.getIntExtra("notificationId", -1) > -1 && it.containsKey(Constants.ENTRY_EXTRA) }
                ?.getLong(Constants.ENTRY_EXTRA)
                ?: return IntentProfileRouting.READY
            entryMergeNavigationFeature.resolveLegacyNotification(legacyNotificationEntryId)
                ?.requestedSubject
                ?.profileId
                ?: return IntentProfileRouting.REJECTED
        }
        if (profileId == profileManager.activeProfileId) {
            return IntentProfileRouting.READY
        }
        val profile = profileManager.visibleProfiles.value.firstOrNull { it.id == profileId }
            ?: return IntentProfileRouting.REJECTED
        if (profileManager.profileRequiresAuthNow(profileId)) {
            if (!authenticateProfile(profile)) return IntentProfileRouting.REJECTED
            profileManager.markProfileAuthenticated(profileId)
            SecureActivityDelegate.unlock()
        }
        profilePendingIntent.store(intent)
        profileManager.setActiveProfile(profileId)
        return IntentProfileRouting.SWITCHED
    }

    private suspend fun onHomeNavigator(navigator: Navigator, isLaunch: Boolean) {
        this.navigator = navigator
        if (isLaunch) {
            val launchIntent = profilePendingIntent.consume() ?: intent
            navigator.popUntilRoot()
            handleIntentAction(launchIntent, navigator)
        } else {
            profilePendingIntent.consume()?.let { handleIntentAction(it, navigator) }
        }
    }

    private suspend fun authenticateProfile(profile: Profile): Boolean {
        return authenticate(
            title = this.stringResource(MR.strings.unlock_app_title, profile.name),
            subtitle = null,
        )
    }

    private fun shouldSkipStartupProfileAuth(): Boolean {
        return securityPreferences.useAuthenticator.get() && SecureActivityDelegate.requireUnlock
    }

    companion object {
        const val INTENT_SEARCH = "eu.kanade.tachiyomi.SEARCH"
        const val INTENT_SEARCH_QUERY = "query"
        const val INTENT_SEARCH_FILTER = "filter"
        private const val STATE_STARTUP_COMPLETED = "startup_completed"
        private const val STATE_ALLOW_APP_UNLOCK_PROMPT = "allow_app_unlock_prompt"
    }
}

internal fun resolveShortcutTab(
    action: String?,
    entryIdToOpen: Long? = null,
): HomeScreen.Tab? {
    return when (action) {
        Constants.SHORTCUT_LIBRARY -> HomeScreen.Tab.Library()
        Constants.SHORTCUT_MANGA -> {
            val idToOpen = entryIdToOpen ?: return null
            HomeScreen.Tab.Library(entryIdToOpen = idToOpen)
        }
        Constants.SHORTCUT_ANIME -> {
            val idToOpen = entryIdToOpen ?: return null
            HomeScreen.Tab.Library(entryIdToOpen = idToOpen)
        }
        Constants.SHORTCUT_UPDATES -> HomeScreen.Tab.Updates
        Constants.SHORTCUT_HISTORY -> HomeScreen.Tab.History
        Constants.SHORTCUT_SOURCES -> HomeScreen.Tab.Browse(BrowseTab.Page.Sources)
        Constants.SHORTCUT_EXTENSIONS -> HomeScreen.Tab.Browse(BrowseTab.Page.Extensions)
        Constants.SHORTCUT_DOWNLOADS -> HomeScreen.Tab.More(toDownloads = true)
        else -> null
    }
}

private enum class IntentProfileRouting {
    READY,
    SWITCHED,
    REJECTED,
}

// Splash screen
private const val SPLASH_MAX_DURATION = 5000 // ms
private const val SPLASH_EXIT_ANIM_DURATION = 400L // ms
