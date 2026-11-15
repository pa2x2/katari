package app.katari.gradle.chromium

/** Chromium as the JetBrains Runtime's SDK carries it: the native files of its `jcef` module. */
internal object ChromiumFiles {
    const val MODULE = "jcef"

    /**
     * What the app's Chromium never uses, which the AppImage leaves out: the helper of the sandbox Chromium runs without
     * under JCEF, and the library through which JCEF would run Chromium inside the app's own process, where the app runs
     * it in one of its own.
     */
    val LEFT_OUT = setOf("chrome-sandbox", "libjcef.so")
}
