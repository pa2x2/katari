package eu.kanade.tachiyomi.ui.main

import android.animation.ValueAnimator
import android.app.SearchManager
import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.components.AppStateBanners
import eu.kanade.presentation.components.DownloadedOnlyBannerBackgroundColor
import eu.kanade.presentation.components.IncognitoModeBannerBackgroundColor
import eu.kanade.presentation.components.IndexingBannerBackgroundColor
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.presentation.more.settings.screen.data.RestoreBackupScreen
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.DefaultNavigatorScreenTransition
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.deeplink.DeepLinkScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.more.NewUpdateScreen
import eu.kanade.tachiyomi.ui.more.OnboardingScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.isBenchmarkBuildType
import eu.kanade.tachiyomi.util.system.isNavigationBarNeedsScrim
import eu.kanade.tachiyomi.util.system.updaterEnabled
import eu.kanade.tachiyomi.util.view.setComposeContent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import mihon.core.migration.Migrator
import mihon.entry.interactions.EntryDownloadInteraction
import mihon.entry.interactions.EntryMediaCacheMaintenance
import mihon.entry.interactions.settings.EntryMediaCachePreferences
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfilesPreferences
import mihon.feature.profiles.ui.ProfilePickerScene
import mihon.feature.profiles.ui.switchToProfile
import mihon.feature.support.SupportUsScreen
import tachiyomi.core.common.Constants
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.injectLazy
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.time.times

class MainActivity : BaseActivity() {

    private val libraryPreferences: LibraryPreferences by injectLazy()
    private val mediaCachePreferences: EntryMediaCachePreferences by injectLazy()
    private val preferences: BasePreferences by injectLazy()
    private val uiPreferences: UiPreferences by injectLazy()
    private val securityPreferences: SecurityPreferences by injectLazy()
    private val profileManager: ProfileManager by injectLazy()
    private val profilesPreferences: ProfilesPreferences by injectLazy()

    private val entryDownloadInteraction: EntryDownloadInteraction by injectLazy()
    private val mediaCacheMaintenance: EntryMediaCacheMaintenance by injectLazy()
    private val extensionManager: ExtensionManager by injectLazy()

    private val getIncognitoState: GetIncognitoState by injectLazy()

    // To be checked by splash screen. If true then splash screen will be removed.
    var ready = false
    private var startupCompleted = false

    private var navigator: Navigator? = null
    private var allowAppUnlockPrompt = true

    init {
        registerSecureActivity(this)
    }

    override fun shouldRequestAppUnlock(activity: AppCompatActivity): Boolean {
        return allowAppUnlockPrompt
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val isFreshLaunch = savedInstanceState == null
        val savedStartupCompleted = savedInstanceState?.getBoolean(STATE_STARTUP_COMPLETED) == true
        val savedAllowAppUnlockPrompt = savedInstanceState
            ?.takeIf { it.containsKey(STATE_ALLOW_APP_UNLOCK_PROMPT) }
            ?.getBoolean(STATE_ALLOW_APP_UNLOCK_PROMPT)
        val startupRestorationDecision = resolveStartupRestorationDecision(
            startupCompleted = savedStartupCompleted,
            restoredAllowAppUnlockPrompt = savedAllowAppUnlockPrompt,
            shouldShowPickerOnLaunch = if (!savedStartupCompleted && savedAllowAppUnlockPrompt == null) {
                runBlocking { profileManager.shouldShowPickerOnLaunch() }
            } else {
                false
            },
        )
        startupCompleted = savedStartupCompleted
        allowAppUnlockPrompt = startupRestorationDecision.allowAppUnlockPrompt

        // Prevent splash screen showing up on configuration changes
        val splashScreen = if (isFreshLaunch) installSplashScreen() else null

        super.onCreate(savedInstanceState)

        Migrator.awaitAndRelease()

        // Do not let the launcher create a new activity http://stackoverflow.com/questions/16283079
        if (!isTaskRoot) {
            finish()
            return
        }

        setComposeContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val activity = context as MainActivity

            var incognito by remember { mutableStateOf(getIncognitoState.await(null)) }
            val downloadOnly by libraryPreferences.downloadedOnly.collectAsState()
            val indexing by entryDownloadInteraction.isInitializing.collectAsState(initial = false)
            val visibleProfiles by profileManager.visibleProfiles.collectAsState()
            val activeProfile by profileManager.activeProfile.collectAsState()
            val activeProfileId = activeProfile?.id ?: profileManager.activeProfileId
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
            val pendingAuthProfile = remember(pendingAuthProfileId, activeProfile, visibleProfiles) {
                when {
                    pendingAuthProfileId == null -> null
                    activeProfile?.id == pendingAuthProfileId -> activeProfile
                    else -> visibleProfiles.firstOrNull { it.id == pendingAuthProfileId }
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
            val statusBarBackgroundColor = when {
                indexing -> IndexingBannerBackgroundColor
                downloadOnly -> DownloadedOnlyBannerBackgroundColor
                incognito -> IncognitoModeBannerBackgroundColor
                else -> MaterialTheme.colorScheme.surface
            }
            LaunchedEffect(isSystemInDarkTheme, statusBarBackgroundColor) {
                // Draw edge-to-edge and set system bars color to transparent
                val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
                val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                enableEdgeToEdge(
                    statusBarStyle = if (statusBarBackgroundColor.luminance() > 0.5) lightStyle else darkStyle,
                    navigationBarStyle = if (isSystemInDarkTheme) darkStyle else lightStyle,
                )
            }

            LaunchedEffect(startupRestorationDecision.shouldResumeStartup, startupGateState) {
                if (!startupRestorationDecision.shouldResumeStartup ||
                    startupGateState != ProfileStartupGateState.Loading
                ) {
                    return@LaunchedEffect
                }

                profileManager.storePendingIntent(intent)
                val initialProfile = profileManager.loadInitialProfile()
                setAppCompatDelegateThemeMode(uiPreferences.themeMode.get())

                val shouldShowPicker =
                    profilesPreferences.pickerEnabled.get() &&
                        profileManager.shouldShowPicker.first()
                val startupDecision = resolveInitialStartupGateDecision(
                    shouldShowPicker = shouldShowPicker,
                    initialProfile = initialProfile,
                    requiresProfileUnlock = initialProfile?.let { profileManager.profileRequiresUnlock(it.id) } == true,
                    shouldSkipProfileAuth = shouldSkipStartupProfileAuth(),
                )
                allowAppUnlockPrompt = startupDecision.allowAppUnlockPrompt
                pendingAuthProfileId = startupDecision.pendingAuthProfile?.id
                startupGateState = startupDecision.state
            }

            LaunchedEffect(startupGateState, visibleProfiles, activeProfile?.id) {
                if (
                    startupGateState == ProfileStartupGateState.Picker &&
                    visibleProfiles.size == 1
                ) {
                    val profile = activeProfile ?: visibleProfiles.firstOrNull()
                    val startupDecision = resolvePickerCollapseStartupGateDecision(
                        profile = profile,
                        requiresProfileUnlock = profile?.let { profileManager.profileRequiresUnlock(it.id) } == true,
                        shouldSkipProfileAuth = shouldSkipStartupProfileAuth(),
                    )
                    allowAppUnlockPrompt = startupDecision.allowAppUnlockPrompt
                    pendingAuthProfileId = startupDecision.pendingAuthProfile?.id
                    if (startupDecision.state == ProfileStartupGateState.Ready) {
                        setAppCompatDelegateThemeMode(uiPreferences.themeMode.get())
                    }
                    startupGateState = startupDecision.state
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
                if (startupGateState != ProfileStartupGateState.Loading) {
                    ready = true
                }
            }

            if (startupGateState == ProfileStartupGateState.Ready) {
                Navigator(
                    screen = HomeScreen,
                    key = "MainNavigator:$activeProfileId",
                    disposeBehavior = NavigatorDisposeBehavior(disposeNestedNavigators = false, disposeSteps = true),
                ) { navigator ->
                    LaunchedEffect(navigator, startupGateState) {
                        this@MainActivity.navigator = navigator
                        completeStartup(startupRestorationDecision.shouldResumeStartup, navigator)
                    }
                    LaunchedEffect(navigator.lastItem) {
                        (navigator.lastItem as? CatalogScreen)?.sourceId
                            .let(getIncognitoState::subscribe)
                            .collectLatest { incognito = it }
                    }

                    val scaffoldInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)
                    Scaffold(
                        topBar = {
                            AppStateBanners(
                                downloadedOnlyMode = downloadOnly,
                                incognitoMode = incognito,
                                indexing = indexing,
                                modifier = Modifier.windowInsetsPadding(scaffoldInsets),
                            )
                        },
                        contentWindowInsets = scaffoldInsets,
                    ) { contentPadding ->
                        // Consume insets already used by app state banners
                        Box {
                            // Shows current screen
                            DefaultNavigatorScreenTransition(
                                navigator = navigator,
                                modifier = Modifier
                                    .padding(contentPadding)
                                    .consumeWindowInsets(contentPadding),
                            )

                            // Draw navigation bar scrim when needed
                            if (remember { isNavigationBarNeedsScrim() }) {
                                Spacer(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                                        .alpha(0.8f)
                                        .background(MaterialTheme.colorScheme.surfaceContainer),
                                )
                            }
                        }
                    }

                    // Pop source-related screens when incognito mode is turned off
                    LaunchedEffect(Unit) {
                        preferences.incognitoMode.changes()
                            .drop(1)
                            .filter { !it }
                            .onEach {
                                val currentScreen = navigator.lastItem
                                if (currentScreen is CatalogScreen ||
                                    (currentScreen is EntryScreen && currentScreen.fromSource)
                                ) {
                                    navigator.popUntilRoot()
                                }
                            }
                            .launchIn(this)
                    }

                    HandleOnNewIntent(context = context, navigator = navigator)

                    if (!isBenchmarkBuildType) {
                        CheckForUpdates()
                        ShowOnboarding()
                        ShowDonationCampaign()
                    }
                }
            } else {
                ProfileGateContent(
                    state = startupGateState,
                    profiles = visibleProfiles,
                    activeProfileId = activeProfile?.id,
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

        val startTime = System.currentTimeMillis()
        splashScreen?.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - startTime
            elapsed <= SPLASH_MIN_DURATION || (!ready && elapsed <= SPLASH_MAX_DURATION)
        }
        setSplashScreenExitAnimation(splashScreen)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_STARTUP_COMPLETED, startupCompleted)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!startupCompleted) {
            profileManager.storePendingIntent(intent)
        }
    }

    @Composable
    private fun HandleOnNewIntent(context: Context, navigator: Navigator) {
        LaunchedEffect(Unit) {
            callbackFlow {
                val componentActivity = context as ComponentActivity
                val consumer = Consumer<Intent> { trySend(it) }
                componentActivity.addOnNewIntentListener(consumer)
                awaitClose { componentActivity.removeOnNewIntentListener(consumer) }
            }
                .collectLatest {
                    if (!startupCompleted) {
                        profileManager.storePendingIntent(it)
                    }
                    if (startupCompleted) {
                        handleIntentAction(it, navigator)
                    }
                }
        }
    }

    @Composable
    private fun CheckForUpdates() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        // App updates
        LaunchedEffect(Unit) {
            if (updaterEnabled) {
                try {
                    val result = AppUpdateChecker().checkForUpdate(context)
                    if (result is GetApplicationRelease.Result.NewUpdate) {
                        val updateScreen = NewUpdateScreen(
                            versionName = result.release.version,
                            changelogInfo = result.release.info,
                            releaseLink = result.release.releaseLink,
                            downloadLink = result.release.downloadLink,
                        )
                        navigator.push(updateScreen)
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e)
                }
            }
        }
    }

    @Composable
    private fun ShowOnboarding() {
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(Unit) {
            if (!preferences.shownOnboardingFlow.get() && navigator.lastItem !is OnboardingScreen) {
                navigator.push(OnboardingScreen())
            }
        }
    }

    @Composable
    private fun ShowDonationCampaign() {
        val navigator = LocalNavigator.currentOrThrow

        var showCampaign by remember { mutableStateOf(false) }
        if (showCampaign) {
            val uriHandler = LocalUriHandler.current
            val dismissSupportMessage = {
                preferences.donationCampaignShown.set(true)
                showCampaign = false
            }
            AdaptiveSheet(
                onDismissRequest = dismissSupportMessage,
                enableImplicitDismiss = false,
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(MR.strings.donationCampaign_title),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = stringResource(MR.strings.donationCampaign_paragraph1),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(MR.strings.donationCampaign_paragraph2),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(MR.strings.donationCampaign_paragraph3),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    HorizontalDivider()

                    Button(
                        modifier = Modifier
                            .padding(top = MaterialTheme.padding.small)
                            .padding(horizontal = MaterialTheme.padding.medium)
                            .fillMaxWidth(),
                        onClick = {
                            navigator.push(SupportUsScreen())
                            dismissSupportMessage()
                        },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolunteerActivism,
                                contentDescription = null,
                            )
                            Text(
                                text = stringResource(MR.strings.label_support_us),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        modifier = Modifier
                            .padding(bottom = MaterialTheme.padding.small)
                            .padding(horizontal = MaterialTheme.padding.medium),
                    ) {
                        OutlinedButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            onClick = { uriHandler.openUri(Constants.URL_DISCORD) },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                            ) {
                                Text(
                                    text = stringResource(MR.strings.donationCampaign_contactPlatform),
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.OpenInNew,
                                    contentDescription = null,
                                )
                            }
                        }
                        OutlinedButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            onClick = dismissSupportMessage,
                        ) {
                            Text(
                                text = stringResource(MR.strings.donationCampaign_dismiss),
                            )
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            try {
                val firstInstallTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime
                val eligibleTime = Instant.fromEpochMilliseconds(firstInstallTime).plus(6 * 30.days)
                showCampaign = (Clock.System.now() >= eligibleTime && !preferences.donationCampaignShown.get())
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
    }

    /**
     * Sets custom splash screen exit animation on devices prior to Android 12.
     *
     * When custom animation is used, status and navigation bar color will be set to transparent and will be restored
     * after the animation is finished.
     */
    @Suppress("Deprecation")
    private fun setSplashScreenExitAnimation(splashScreen: SplashScreen?) {
        val root = findViewById<View>(android.R.id.content)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && splashScreen != null) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT

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
        val notificationId = intent.getIntExtra("notificationId", -1)
        if (notificationId > -1) {
            NotificationReceiver.dismissNotification(
                applicationContext,
                notificationId,
                intent.getIntExtra("groupId", 0),
            )
        }

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
                if (intent.action == Constants.SHORTCUT_EXTENSIONS) {
                    // Profile-type routing removed in Phase 4; extensions open in the active profile.
                }

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
                navigator.popUntilRoot()
                navigator.push(SettingsScreen())
                null
            }
            Intent.ACTION_SEARCH, Intent.ACTION_SEND, "com.google.android.gms.actions.SEARCH_ACTION" -> {
                // If the intent match the "standard" Android search intent
                // or the Google-specific search intent (triggered by saying or typing "search *query* on *Tachiyomi*" in Google Search/Google Assistant)

                // Get the search query provided in extras, and if not null, perform a global search with it.
                val query = intent.getStringExtra(SearchManager.QUERY) ?: intent.getStringExtra(Intent.EXTRA_TEXT)
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
                val data = intent.data
                // Handling opening of backup files
                if (data.toString().endsWith(".tachibk")) {
                    navigator.popUntilRoot()
                    navigator.push(RestoreBackupScreen(data.toString()))
                }
                // Deep link to an entry
                else if (data?.path?.startsWith("/entry/") == true) {
                    val entryId = data.pathSegments.lastOrNull()?.toLongOrNull()
                    if (entryId != null) {
                        navigator.popUntilRoot()
                        navigator.push(EntryScreen(entryId, fromSource = true))
                    }
                }
                // Deep link to add extension store
                else if (intent.isAddExtensionStoreIntent()) {
                    data?.getQueryParameter("url")?.let { repoUrl ->
                        navigator.popUntilRoot()
                        navigator.push(ExtensionStoresScreen(repoUrl))
                    }
                }
                null
            }
            else -> return false
        }

        if (tabToOpen != null) {
            lifecycleScope.launch { HomeScreen.openTab(tabToOpen) }
        }

        return true
    }

    private suspend fun completeStartup(isLaunch: Boolean, navigator: Navigator) {
        if (startupCompleted) {
            ready = true
            return
        }

        startupCompleted = true

        if (isLaunch) {
            val launchIntent = profileManager.consumePendingIntent() ?: intent
            navigator.popUntilRoot()
            handleIntentAction(launchIntent, navigator)

            // Reset Incognito Mode on relaunch
            preferences.incognitoMode.set(false)

            enabledMediaCacheBucketsForLaunchClear(
                autoClearEntryPageImageCache = mediaCachePreferences.autoClearEntryPageImageCache.get(),
                autoClearAnimePlaybackCache = mediaCachePreferences.autoClearAnimePlaybackCache.get(),
            ).forEach { bucketKey ->
                lifecycleScope.launchIO {
                    mediaCacheMaintenance.clear(bucketKey)
                }
            }

            lifecycleScope.launchIO {
                try {
                    extensionManager.checkForUpdates(applicationContext)
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e)
                }
            }

            LibraryUpdateJob.setupTask(
                context = this,
                prefInterval = libraryPreferences.autoUpdateInterval.get(),
            )
        }

        ready = true
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

    private fun Intent.isAddExtensionStoreIntent(): Boolean {
        return (scheme == "tachiyomi" && data?.host == "add-repo") ||
            (scheme == "katari" && data?.host == "extension-store")
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
        Constants.SHORTCUT_SOURCES -> HomeScreen.Tab.Browse(false)
        Constants.SHORTCUT_EXTENSIONS -> HomeScreen.Tab.Browse(true)
        Constants.SHORTCUT_DOWNLOADS -> HomeScreen.Tab.More(toDownloads = true)
        else -> null
    }
}

internal data class ProfileStartupDecision(
    val allowAppUnlockPrompt: Boolean,
    val state: ProfileStartupGateState,
    val pendingAuthProfile: Profile?,
)

internal data class StartupRestorationDecision(
    val shouldResumeStartup: Boolean,
    val allowAppUnlockPrompt: Boolean,
)

internal fun resolveStartupRestorationDecision(
    startupCompleted: Boolean,
    restoredAllowAppUnlockPrompt: Boolean?,
    shouldShowPickerOnLaunch: Boolean,
): StartupRestorationDecision {
    return StartupRestorationDecision(
        shouldResumeStartup = !startupCompleted,
        allowAppUnlockPrompt = when {
            startupCompleted -> true
            restoredAllowAppUnlockPrompt != null -> restoredAllowAppUnlockPrompt
            else -> !shouldShowPickerOnLaunch
        },
    )
}

internal fun resolveInitialStartupGateDecision(
    shouldShowPicker: Boolean,
    initialProfile: Profile?,
    requiresProfileUnlock: Boolean,
    shouldSkipProfileAuth: Boolean,
): ProfileStartupDecision {
    return when {
        shouldShowPicker -> ProfileStartupDecision(
            allowAppUnlockPrompt = false,
            state = ProfileStartupGateState.Picker,
            pendingAuthProfile = null,
        )
        initialProfile != null && requiresProfileUnlock && !shouldSkipProfileAuth -> ProfileStartupDecision(
            allowAppUnlockPrompt = true,
            state = ProfileStartupGateState.Authenticating,
            pendingAuthProfile = initialProfile,
        )
        else -> ProfileStartupDecision(
            allowAppUnlockPrompt = true,
            state = ProfileStartupGateState.Ready,
            pendingAuthProfile = null,
        )
    }
}

internal fun resolvePickerCollapseStartupGateDecision(
    profile: Profile?,
    requiresProfileUnlock: Boolean,
    shouldSkipProfileAuth: Boolean,
): ProfileStartupDecision {
    return if (profile != null && requiresProfileUnlock && !shouldSkipProfileAuth) {
        ProfileStartupDecision(
            allowAppUnlockPrompt = true,
            state = ProfileStartupGateState.Authenticating,
            pendingAuthProfile = profile,
        )
    } else {
        ProfileStartupDecision(
            allowAppUnlockPrompt = true,
            state = ProfileStartupGateState.Ready,
            pendingAuthProfile = null,
        )
    }
}

internal enum class ProfileStartupGateState {
    Loading,
    Picker,
    Authenticating,
    Ready,
}

@Composable
private fun ProfileGateContent(
    state: ProfileStartupGateState,
    profiles: List<Profile>,
    activeProfileId: Long?,
    authProfileName: String?,
    onProfileSelected: (Profile) -> Unit,
) {
    when {
        state == ProfileStartupGateState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        state == ProfileStartupGateState.Authenticating -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    authProfileName?.let {
                        Text(
                            text = stringResource(MR.strings.unlock_app_title, it),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
        state == ProfileStartupGateState.Picker -> {
            if (profiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                ProfilePickerScene(
                    profiles = profiles,
                    activeProfileId = activeProfileId,
                    onProfileSelected = onProfileSelected,
                    onOpenManagement = null,
                )
            }
        }
        else -> Unit
    }
}

// Splash screen
private const val SPLASH_MIN_DURATION = 500 // ms
private const val SPLASH_MAX_DURATION = 5000 // ms
private const val SPLASH_EXIT_ANIM_DURATION = 400L // ms
