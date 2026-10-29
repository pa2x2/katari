package mihon.extension.compat

import android.os.Build

/**
 * The system properties Android's runtime sets in every app process beyond the JDK's own. Extensions send `http.agent`
 * as their user agent and read it as never null, which fails on the JVM, where it is unset.
 */
internal object AndroidSystemProperties {

    fun install() {
        System.setProperty("http.agent", dalvikUserAgent())
    }

    /** What `RuntimeInit.getDefaultUserAgent` makes of the stand-ins' `Build`, with ART's VM version. */
    private fun dalvikUserAgent(): String = buildString {
        append("Dalvik/").append(ART_VM_VERSION)
        append(" (Linux; U; Android ").append(Build.VERSION.RELEASE.ifEmpty { "1.0" })
        if (Build.VERSION.CODENAME == "REL" && Build.MODEL.isNotEmpty()) append("; ").append(Build.MODEL)
        if (Build.ID.isNotEmpty()) append(" Build/").append(Build.ID)
        append(")")
    }

    // ART's java.vm.version, which Dalvik's user agent names; the JDK's would read as no Android release
    private const val ART_VM_VERSION = "2.1.0"
}
