package app.katari.ui.host

/**
 * Marks what every platform's host calls to start and run the app, such as the phases of [AppProcessStart] and
 * [HomeHost]. The build fails when the Android app or the desktop app doesn't call one, so start-up and launch work
 * written once can't be left out on one platform.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class HostEntryPoint
