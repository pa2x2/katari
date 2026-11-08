package mihon.extension.compat.webkit

import android.webkit.WebSettings
import mihon.core.common.web.ExtensionPageSettings
import mihon.extension.compat.AndroidCompatEnvironment

/**
 * A web view's settings, starting as Android's web view's do for an app of today's SDK. Each setting reads back what
 * was set; the ones a hidden page acts on, its user agent, whether its scripts run and whether it loads images from the
 * network, reach it through [onChange] as they change. The rest change nothing a page nobody sees or touches does.
 * Calls may come from any thread, as on Android.
 *
 * @param onChange gets the page's settings after each change of one it acts on, on the thread that changed it.
 */
class PageWebSettings internal constructor(
    private val defaultUserAgent: String,
    private val onChange: (ExtensionPageSettings) -> Unit,
) : WebSettings() {
    private var userAgent: String = defaultUserAgent
    private var javaScriptEnabled = false
    private var blockNetworkImage = false
    private var loadsImagesAutomatically = true

    private var allowContentAccess = true
    private var allowFileAccess = false
    private var allowFileAccessFromFileUrls = false
    private var allowUniversalAccessFromFileUrls = false
    private var blockNetworkLoads = false
    private var builtInZoomControls = false
    private var cacheMode = LOAD_DEFAULT
    private var cursiveFontFamily = "cursive"
    private var databaseEnabled = false
    private var databasePath = ""
    private var defaultFixedFontSize = 13
    private var defaultFontSize = 16
    private var defaultTextEncodingName = "UTF-8"
    private var defaultZoom = ZoomDensity.MEDIUM
    private var disabledActionModeMenuItems = MENU_ITEM_NONE
    private var displayZoomControls = true
    private var domStorageEnabled = false
    private var fantasyFontFamily = "fantasy"
    private var fixedFontFamily = "monospace"
    private var javaScriptCanOpenWindowsAutomatically = false
    private var layoutAlgorithm = LayoutAlgorithm.NARROW_COLUMNS
    private var lightTouchEnabled = false
    private var loadWithOverviewMode = false
    private var mediaPlaybackRequiresUserGesture = true
    private var minimumFontSize = 8
    private var minimumLogicalFontSize = 8
    private var mixedContentMode = MIXED_CONTENT_NEVER_ALLOW
    private var offscreenPreRaster = false
    private var pluginState = PluginState.OFF
    private var safeBrowsingEnabled = true
    private var sansSerifFontFamily = "sans-serif"
    private var saveFormData = false
    private var savePassword = false
    private var serifFontFamily = "serif"
    private var standardFontFamily = "sans-serif"
    private var supportMultipleWindows = false
    private var supportZoom = true
    private var smoothTransition = false
    private var textZoom = 100
    private var useWideViewPort = false

    @Synchronized
    fun toPageSettings() = ExtensionPageSettings(
        userAgent = userAgent,
        javaScriptEnabled = javaScriptEnabled,
        blocksImages = blockNetworkImage || !loadsImagesAutomatically,
    )

    private fun changed() {
        onChange(toPageSettings())
    }

    @Synchronized
    override fun getUserAgentString(): String = userAgent

    // As on Android, null or empty sets the default back
    override fun setUserAgentString(value: String?) {
        synchronized(this) { userAgent = value?.takeIf { it.isNotEmpty() } ?: defaultUserAgent }
        changed()
    }

    @Synchronized
    override fun getJavaScriptEnabled(): Boolean = javaScriptEnabled

    override fun setJavaScriptEnabled(flag: Boolean) {
        synchronized(this) { javaScriptEnabled = flag }
        changed()
    }

    @Synchronized
    override fun getBlockNetworkImage(): Boolean = blockNetworkImage

    override fun setBlockNetworkImage(flag: Boolean) {
        synchronized(this) { blockNetworkImage = flag }
        changed()
    }

    @Synchronized
    override fun getLoadsImagesAutomatically(): Boolean = loadsImagesAutomatically

    override fun setLoadsImagesAutomatically(flag: Boolean) {
        synchronized(this) { loadsImagesAutomatically = flag }
        changed()
    }

    @Synchronized override fun getAllowContentAccess() = allowContentAccess

    @Synchronized override fun setAllowContentAccess(flag: Boolean) {
        allowContentAccess = flag
    }

    @Synchronized override fun getAllowFileAccess() = allowFileAccess

    @Synchronized override fun setAllowFileAccess(flag: Boolean) {
        allowFileAccess = flag
    }

    @Synchronized override fun getAllowFileAccessFromFileURLs() = allowFileAccessFromFileUrls

    @Synchronized override fun setAllowFileAccessFromFileURLs(flag: Boolean) {
        allowFileAccessFromFileUrls = flag
    }

    @Synchronized override fun getAllowUniversalAccessFromFileURLs() = allowUniversalAccessFromFileUrls

    @Synchronized override fun setAllowUniversalAccessFromFileURLs(flag: Boolean) {
        allowUniversalAccessFromFileUrls = flag
    }

    @Synchronized override fun getBlockNetworkLoads() = blockNetworkLoads

    @Synchronized override fun setBlockNetworkLoads(flag: Boolean) {
        blockNetworkLoads = flag
    }

    @Synchronized override fun getBuiltInZoomControls() = builtInZoomControls

    @Synchronized override fun setBuiltInZoomControls(flag: Boolean) {
        builtInZoomControls = flag
    }

    @Synchronized override fun getCacheMode() = cacheMode

    @Synchronized override fun setCacheMode(value: Int) {
        cacheMode = value
    }

    @Synchronized override fun getCursiveFontFamily() = cursiveFontFamily

    @Synchronized override fun setCursiveFontFamily(value: String) {
        cursiveFontFamily = value
    }

    @Synchronized override fun getDatabaseEnabled() = databaseEnabled

    @Synchronized override fun setDatabaseEnabled(flag: Boolean) {
        databaseEnabled = flag
    }

    @Synchronized override fun getDatabasePath() = databasePath

    @Synchronized override fun setDatabasePath(value: String) {
        databasePath = value
    }

    @Synchronized override fun getDefaultFixedFontSize() = defaultFixedFontSize

    @Synchronized override fun setDefaultFixedFontSize(value: Int) {
        defaultFixedFontSize = value
    }

    @Synchronized override fun getDefaultFontSize() = defaultFontSize

    @Synchronized override fun setDefaultFontSize(value: Int) {
        defaultFontSize = value
    }

    @Synchronized override fun getDefaultTextEncodingName() = defaultTextEncodingName

    @Synchronized override fun setDefaultTextEncodingName(value: String) {
        defaultTextEncodingName = value
    }

    @Synchronized override fun getDefaultZoom(): ZoomDensity = defaultZoom

    @Synchronized override fun setDefaultZoom(value: ZoomDensity) {
        defaultZoom = value
    }

    @Synchronized override fun getDisabledActionModeMenuItems() = disabledActionModeMenuItems

    @Synchronized override fun setDisabledActionModeMenuItems(value: Int) {
        disabledActionModeMenuItems = value
    }

    @Synchronized override fun getDisplayZoomControls() = displayZoomControls

    @Synchronized override fun setDisplayZoomControls(flag: Boolean) {
        displayZoomControls = flag
    }

    // Chromium keeps a page's storage either way
    @Synchronized override fun getDomStorageEnabled() = domStorageEnabled

    @Synchronized override fun setDomStorageEnabled(flag: Boolean) {
        domStorageEnabled = flag
    }

    @Synchronized override fun getFantasyFontFamily() = fantasyFontFamily

    @Synchronized override fun setFantasyFontFamily(value: String) {
        fantasyFontFamily = value
    }

    @Synchronized override fun getFixedFontFamily() = fixedFontFamily

    @Synchronized override fun setFixedFontFamily(value: String) {
        fixedFontFamily = value
    }

    @Synchronized override fun getJavaScriptCanOpenWindowsAutomatically() = javaScriptCanOpenWindowsAutomatically

    @Synchronized override fun setJavaScriptCanOpenWindowsAutomatically(flag: Boolean) {
        javaScriptCanOpenWindowsAutomatically = flag
    }

    @Synchronized override fun getLayoutAlgorithm(): LayoutAlgorithm = layoutAlgorithm

    @Synchronized override fun setLayoutAlgorithm(value: LayoutAlgorithm) {
        layoutAlgorithm = value
    }

    @Synchronized override fun getLightTouchEnabled() = lightTouchEnabled

    @Synchronized override fun setLightTouchEnabled(flag: Boolean) {
        lightTouchEnabled = flag
    }

    @Synchronized override fun getLoadWithOverviewMode() = loadWithOverviewMode

    @Synchronized override fun setLoadWithOverviewMode(flag: Boolean) {
        loadWithOverviewMode = flag
    }

    @Synchronized override fun getMediaPlaybackRequiresUserGesture() = mediaPlaybackRequiresUserGesture

    @Synchronized override fun setMediaPlaybackRequiresUserGesture(flag: Boolean) {
        mediaPlaybackRequiresUserGesture = flag
    }

    @Synchronized override fun getMinimumFontSize() = minimumFontSize

    @Synchronized override fun setMinimumFontSize(value: Int) {
        minimumFontSize = value
    }

    @Synchronized override fun getMinimumLogicalFontSize() = minimumLogicalFontSize

    @Synchronized override fun setMinimumLogicalFontSize(value: Int) {
        minimumLogicalFontSize = value
    }

    @Synchronized override fun getMixedContentMode() = mixedContentMode

    @Synchronized override fun setMixedContentMode(value: Int) {
        mixedContentMode = value
    }

    @Synchronized override fun getOffscreenPreRaster() = offscreenPreRaster

    @Synchronized override fun setOffscreenPreRaster(flag: Boolean) {
        offscreenPreRaster = flag
    }

    @Synchronized override fun getPluginState(): PluginState = pluginState

    @Synchronized override fun setPluginState(value: PluginState) {
        pluginState = value
    }

    @Synchronized override fun getSafeBrowsingEnabled() = safeBrowsingEnabled

    @Synchronized override fun setSafeBrowsingEnabled(flag: Boolean) {
        safeBrowsingEnabled = flag
    }

    @Synchronized override fun getSansSerifFontFamily() = sansSerifFontFamily

    @Synchronized override fun setSansSerifFontFamily(value: String) {
        sansSerifFontFamily = value
    }

    @Synchronized override fun getSaveFormData() = saveFormData

    @Synchronized override fun setSaveFormData(flag: Boolean) {
        saveFormData = flag
    }

    @Synchronized override fun getSavePassword() = savePassword

    @Synchronized override fun setSavePassword(flag: Boolean) {
        savePassword = flag
    }

    @Synchronized override fun getSerifFontFamily() = serifFontFamily

    @Synchronized override fun setSerifFontFamily(value: String) {
        serifFontFamily = value
    }

    @Synchronized override fun getStandardFontFamily() = standardFontFamily

    @Synchronized override fun setStandardFontFamily(value: String) {
        standardFontFamily = value
    }

    @Synchronized override fun supportMultipleWindows() = supportMultipleWindows

    @Synchronized override fun setSupportMultipleWindows(flag: Boolean) {
        supportMultipleWindows = flag
    }

    @Synchronized override fun supportZoom() = supportZoom

    @Synchronized override fun setSupportZoom(flag: Boolean) {
        supportZoom = flag
    }

    @Synchronized override fun enableSmoothTransition() = smoothTransition

    @Synchronized override fun setEnableSmoothTransition(flag: Boolean) {
        smoothTransition = flag
    }

    @Synchronized override fun getTextZoom() = textZoom

    @Synchronized override fun setTextZoom(value: Int) {
        textZoom = value
    }

    @Synchronized override fun getUseWideViewPort() = useWideViewPort

    @Synchronized override fun setUseWideViewPort(flag: Boolean) {
        useWideViewPort = flag
    }

    // A page nobody sees asks for no location, takes no focus and has no other views to be put before
    override fun setGeolocationDatabasePath(value: String) = Unit

    override fun setGeolocationEnabled(flag: Boolean) = Unit

    override fun setNeedInitialFocus(flag: Boolean) = Unit

    override fun setRenderPriority(value: RenderPriority) = Unit

    companion object {
        /** The user agent of a web view whose extension sets none, [WebSettings.getDefaultUserAgent]'s. */
        @JvmStatic
        fun defaultUserAgent(): String = AndroidCompatEnvironment.current().webPages.defaultUserAgent
    }
}
