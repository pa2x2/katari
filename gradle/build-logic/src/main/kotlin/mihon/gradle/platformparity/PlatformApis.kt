package mihon.gradle.platformparity

import java.io.File

/**
 * The names from outside the repository that each platform's code can use, as `indexPlatformApis` writes them. The
 * repository's own declarations aren't here: the checks read them from the sources, so editing a source file leaves
 * the index up to date.
 *
 * [androidStandIns] are the desktop stand-ins for the Android API and the stub of `android.jar` behind them. Desktop
 * code in the modules whose shared code compiles against the Android API (D063) sees them; everywhere else on desktop
 * an Android class stays unavailable.
 */
internal data class PlatformApis(
    val desktop: PlatformApiNames,
    val android: PlatformApiNames,
    val androidStandIns: PlatformApiNames,
) {
    fun write(file: File) {
        file.bufferedWriter().use { writer ->
            sections().forEach { (section, names) ->
                writer.appendLine("$SECTION_PREFIX$section")
                names.write(writer)
            }
        }
    }

    private fun sections() = listOf(DESKTOP to desktop, ANDROID to android, ANDROID_STAND_INS to androidStandIns)

    companion object {
        val EMPTY = PlatformApis(PlatformApiNames.EMPTY, PlatformApiNames.EMPTY, PlatformApiNames.EMPTY)

        fun read(file: File): PlatformApis {
            val sections = mutableMapOf<String, MutableList<String>>()
            var section = mutableListOf<String>()
            file.forEachLine { line ->
                if (line.startsWith(SECTION_PREFIX)) {
                    section = sections.getOrPut(line.removePrefix(SECTION_PREFIX), ::mutableListOf)
                } else {
                    section += line
                }
            }
            fun names(name: String) = PlatformApiNames.read(sections[name].orEmpty())
            return PlatformApis(names(DESKTOP), names(ANDROID), names(ANDROID_STAND_INS))
        }

        private const val SECTION_PREFIX = "# "
        private const val DESKTOP = "desktop"
        private const val ANDROID = "android"
        private const val ANDROID_STAND_INS = "android-stand-ins"
    }
}
