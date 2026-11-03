package mihon.desktop.application

import tachiyomi.i18n.MR
import tachiyomi.i18n.app_name
import tachiyomi.i18n.localized.stringResource

/**
 * Which build of the app this is, as the Android app's build types are. Linux keeps no app's files apart from another's,
 * so a release installed next to a build made while working on the app stays apart from it only by the names each one
 * owns: its folders, and in them its single-instance lock, its keys in the keyring, its application ID and the name its
 * window and notifications show. Gradle builds the release with `-Pdesktop-release`; every other build is the
 * development one, so running the app from the build tool never opens the installed release's library.
 */
enum class DesktopAppVariant(
    /** The name of the app's own folder in each XDG base directory. */
    val directoryName: String,
    val applicationId: String,
) {
    RELEASE(directoryName = "katari", applicationId = "app.katari.desktop"),
    DEVELOPMENT(directoryName = "katari-dev", applicationId = "app.katari.desktop.dev"),
    ;

    /** The app's name as the desktop shows it, telling the two builds apart where both are open. */
    val displayName: String
        get() {
            val appName = stringResource(MR.strings.app_name)
            return when (this) {
                RELEASE -> appName
                DEVELOPMENT -> "$appName Dev"
            }
        }

    companion object {
        val current: DesktopAppVariant = if (DesktopBuildInfo.IS_RELEASE) RELEASE else DEVELOPMENT
    }
}
