package app.katari.tools.extensioncompatibility.results

import kotlinx.serialization.Serializable

/** How an extension, a source or one call ended, with what it involved where that's known. */
@Serializable
data class Outcome(
    val kind: OutcomeKind,
    /** In words: the error's message, the install step, what timed out. */
    val detail: String = "",
    /** The class or member desktop lacks, or the method it couldn't convert. */
    val member: String? = null,
    /** The error's class, or `HTTP <code>`. */
    val errorType: String? = null,
    val url: String? = null,
) {
    val isWorking: Boolean get() = kind == OutcomeKind.WORKING

    companion object {
        val Working = Outcome(OutcomeKind.WORKING)
    }
}

/** Ordered as the report lists them: working first, then what desktop owes, then what the extension or site does. */
@Serializable
enum class OutcomeKind(val label: String) {
    WORKING("Loaded and working"),
    PLATFORM_GAP("Not supported on desktop yet"),
    NEEDS_WEB_VIEW("Needs the web view or Cloudflare"),
    CONVERSION_FAILED("Dex conversion failed"),
    VERIFICATION_FAILED("Verification or signature failed"),
    INSTALL_FAILED("Install failed otherwise"),
    LOAD_FAILED("Not loaded otherwise"),
    EXTENSION_ERROR("The extension's own error"),
    NEEDS_PERSON("The site's check needs a person"),
    TIMEOUT("Timed out"),
    NOT_INSTALLABLE("Library version or torrent the app doesn't run"),
    FAMILY_NOT_ENABLED("Family not enabled on desktop yet"),
}
