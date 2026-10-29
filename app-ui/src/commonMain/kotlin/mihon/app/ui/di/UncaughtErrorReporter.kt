package mihon.app.ui.di

/**
 * Where a failure in the app scope goes besides the log in builds that don't crash on it. Android reports it to
 * telemetry; a platform without a crash reporter contributes none.
 */
fun interface UncaughtErrorReporter {
    fun report(throwable: Throwable)
}
