package eu.kanade.tachiyomi.util

/** Graph entry point of the crash and debug info screens, which compose outside any view model. */
interface CrashLogGraph {
    val crashLogUtilFactory: CrashLogUtil.Factory
}
