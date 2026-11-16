package app.katari.gradle.chromium

import org.gradle.api.GradleException
import java.io.File

/** What a JetBrains Runtime's `release` file says of the JCEF it carries: the versions of the projects it is made of. */
internal class JcefRelease(release: File) {
    private val lines = release.readLines()

    /** 150.0.14.1233, of 150.0.14.1233+g9df0879. */
    val jcefVersion: String get() = value(VERSION_KEY).substringBefore('+')

    /** 150.0.14, of 150.0.14-g7c1aa68-chromium-150.0.7871.129-api-1.21-263-b17. */
    val cefVersion: String get() = value(DETAILED_KEY).substringBefore('-')

    /** 150.0.7871.129, of the same. */
    val chromiumVersion: String
        get() = value(DETAILED_KEY).substringAfter(CHROMIUM_MARK, "").substringBefore('-').ifEmpty {
            throw GradleException("The JetBrains Runtime's $DETAILED_KEY names no Chromium version")
        }

    private fun value(key: String) = lines
        .firstNotNullOfOrNull { line -> line.removePrefix("$key=").takeIf { it != line } }
        ?.trim('"')
        ?: throw GradleException("The JetBrains Runtime's release file names no $key")

    private companion object {
        const val VERSION_KEY = "JCEF_VERSION"
        const val DETAILED_KEY = "JCEF_VERSION_DETAILED"
        const val CHROMIUM_MARK = "-chromium-"
    }
}
