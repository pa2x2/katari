package app.katari.webview.jcef.devtools

import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * What DevTools' `setUserAgentOverride` takes to make a target [userAgent] in all a site can ask: the header and
 * `navigator.userAgent`, `navigator.platform`, and for a Chrome user agent the client hints (the `Sec-CH-UA` headers
 * and `navigator.userAgentData`), which Chromium otherwise stops sending once the user agent is overridden. A Chrome
 * that sends no hints, or names one system in its user agent and another in its platform, is what a site takes for a
 * program.
 */
internal fun userAgentOverride(userAgent: String): JsonObject = buildJsonObject {
    put("userAgent", userAgent)
    val system = UserAgentSystem.of(userAgent)
    put("platform", system.navigatorPlatform)
    val chrome = CHROME_VERSION.find(userAgent) ?: return@buildJsonObject
    val major = chrome.groupValues[1]
    val full = major + chrome.groupValues[2].ifEmpty { ".0.0.0" }
    val isMobile = "Mobile" in userAgent
    putJsonObject("userAgentMetadata") {
        putJsonArray("brands") { BRANDS.forEach { brand -> addBrand(brand, major, UNKNOWN_BRAND_VERSION) } }
        putJsonArray("fullVersionList") {
            BRANDS.forEach { brand -> addBrand(brand, full, "$UNKNOWN_BRAND_VERSION.0.0.0") }
        }
        put("platform", system.hintPlatform)
        put("platformVersion", system.version(userAgent))
        // Chrome on a phone tells neither; its user agent's model is the placeholder every phone sends
        put("architecture", if (isMobile) "" else "x86")
        put("bitness", if (isMobile) "" else "64")
        put("model", "")
        put("mobile", isMobile)
    }
}

private fun JsonArrayBuilder.addBrand(brand: String, version: String, unknown: String) {
    addJsonObject {
        put("brand", brand)
        put("version", if (brand == UNKNOWN_BRAND) unknown else version)
    }
}

/** The system a user agent names, as the platform string and the client hints tell it. */
private enum class UserAgentSystem(val marker: String, val navigatorPlatform: String, val hintPlatform: String) {
    ANDROID("Android", "Linux armv81", "Android"),
    WINDOWS("Windows", "Win32", "Windows"),
    MAC("Macintosh", "MacIntel", "macOS"),
    LINUX("Linux", "Linux x86_64", "Linux"),
    ;

    /** The system's version for the hints: Android's from the user agent, and none where Chrome sends a fixed one. */
    fun version(userAgent: String): String = when (this) {
        ANDROID -> ANDROID_VERSION.find(userAgent)?.groupValues?.get(1)?.let { "$it.0.0" }.orEmpty()
        else -> ""
    }

    companion object {
        // Android's user agent names Linux too, so it is looked for first
        fun of(userAgent: String): UserAgentSystem = entries.firstOrNull { it.marker in userAgent } ?: LINUX
    }
}

private val CHROME_VERSION = Regex("""Chrome/(\d+)((?:\.\d+)*)""")
private val ANDROID_VERSION = Regex("""Android (\d+)""")

// The brand Chrome adds so that sites don't come to depend on the list's exact content
private const val UNKNOWN_BRAND = "Not=A?Brand"
private const val UNKNOWN_BRAND_VERSION = "24"
private val BRANDS = listOf("Chromium", "Google Chrome", UNKNOWN_BRAND)
