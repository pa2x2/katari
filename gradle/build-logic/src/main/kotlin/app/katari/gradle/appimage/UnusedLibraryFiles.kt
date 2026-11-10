package app.katari.gradle.appimage

import org.gradle.api.GradleException
import java.io.Serializable

/**
 * The files in the jar of the library [group]:[name] that the app never reads where its AppImage runs, which the
 * AppImage leaves out: those whose whole path in the jar matches the regular expression [unused]. [needed] are files
 * beside them that the app does read.
 */
data class UnusedLibraryFiles(
    val group: String,
    val name: String,
    val unused: String,
    val needed: List<String>,
) : Serializable {

    /**
     * Which of [entries], the paths in the library's jar named [jarName], are left out. Fails when none is, or when
     * the jar would be left without one of [needed]: a version of the library that lays its jar out differently
     * would otherwise ship whole, or without what the app loads from it.
     */
    fun leftOutOf(jarName: String, entries: List<String>): Set<String> {
        val pattern = Regex(unused)
        val leftOut = entries.filterTo(mutableSetOf()) { pattern.matches(it) }
        if (leftOut.isEmpty()) {
            throw GradleException("Nothing in $jarName matches $unused, the files the AppImage leaves out of it. $HINT")
        }
        val missing = needed - (entries - leftOut).toSet()
        if (missing.isNotEmpty()) {
            throw GradleException("The AppImage's $jarName would lack $missing, which the app loads from it. $HINT")
        }
        return leftOut
    }

    companion object {
        private const val HINT = "Bring the library's entry in UnusedLibraryFiles.kt in line with the jar's layout."

        /** What the AppImage leaves out of the app's libraries. */
        internal val ALL = listOf(
            // Libraries that carry their native code for every system they run on, of which the AppImage is one
            UnusedLibraryFiles(
                group = "androidx.sqlite",
                name = "sqlite-bundled-jvm",
                unused = "natives/(?!linux_x64/).+",
                needed = listOf("natives/linux_x64/libsqliteJni.so"),
            ),
            UnusedLibraryFiles(
                group = "io.github.pa2x2",
                name = "quickjs-kt-jvm",
                unused = "jni/(?!linux_x64/).+",
                needed = listOf("jni/linux_x64/libquickjs.so"),
            ),
            UnusedLibraryFiles(
                group = "com.squareup.zstd",
                name = "zstd-kmp-jvm",
                unused = """jni/(?!amd64/(libzstd-kmp\.so)?$).+""",
                needed = listOf("jni/amd64/libzstd-kmp.so"),
            ),
            UnusedLibraryFiles(
                group = "net.java.dev.jna",
                name = "jna",
                unused = """com/sun/jna/(?!linux-x86-64/)[^/]+/(lib)?jnidispatch\..+""",
                needed = listOf("com/sun/jna/linux-x86-64/libjnidispatch.so"),
            ),
            // ICU's data for sorting text, for transliteration, and the dictionaries that find the words of languages
            // written without spaces. The app and extensions use ICU to format and parse dates, numbers and lists,
            // which reads none of it.
            UnusedLibraryFiles(
                group = "com.ibm.icu",
                name = "icu4j",
                unused = """com/ibm/icu/impl/data/icudata/((coll|translit)/.+|brkitr/.+\.dict)""",
                needed = emptyList(),
            ),
        )
    }
}
