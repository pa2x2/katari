package mihon.desktop.appearance

import logcat.LogPriority
import mihon.presentation.kit.theme.SystemInterfaceFont
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Linux: the family fontconfig picks for `sans-serif`, read once with `fc-match`, which desktops configure as their
 * default sans font. Without fontconfig's tools there is none.
 */
class FontconfigInterfaceFont : SystemInterfaceFont {
    override val familyName: String? by lazy(::match)

    private fun match(): String? {
        val match = try {
            ProcessBuilder("fc-match", "--format=%{family[0]}", "sans-serif")
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (e: IOException) {
            logcat(LogPriority.DEBUG, e) { "Can't ask fontconfig for the default font through fc-match" }
            return null
        }
        val output = match.inputStream.bufferedReader().use { it.readText() }
        if (!match.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) match.destroy()
        return output.trim().ifEmpty { null }.also { family ->
            if (family == null) logcat(LogPriority.DEBUG) { "fontconfig names no default font" }
        }
    }

    private companion object {
        const val TIMEOUT_SECONDS = 2L
    }
}
