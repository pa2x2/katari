package eu.kanade.presentation.util

import mihon.core.platform.PlatformContext
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource

/*
 * The words for what desktop doesn't provide yet that an extension needed, from the class or member of Android's it
 * found missing (`Extension.NotLoaded.Reason.Failed.missingPlatformMember`, or a source error the desktop recognises).
 */

/** What [missingMember] gives a user: a web view for any of its classes, otherwise the member itself, named. */
fun PlatformContext.platformGapFeature(missingMember: String): String = when {
    WEB_VIEW_PACKAGES.any(missingMember::startsWith) -> stringResource(MR.strings.platform_gap_web_view)
    else -> stringResource(MR.strings.platform_gap_android_member, missingMember)
}

/** Why the extension [extensionName] isn't loaded, when it needed [missingMember]. */
fun PlatformContext.platformGapExtensionMessage(extensionName: String, missingMember: String): String =
    stringResource(MR.strings.platform_gap_extension_message, extensionName, platformGapFeature(missingMember))

/** What a source's call failed for, when it needed [missingMember]. */
fun PlatformContext.platformGapSourceMessage(missingMember: String): String =
    stringResource(MR.strings.platform_gap_source_message, platformGapFeature(missingMember))

private val WEB_VIEW_PACKAGES = listOf("android.webkit.", "androidx.webkit.")
