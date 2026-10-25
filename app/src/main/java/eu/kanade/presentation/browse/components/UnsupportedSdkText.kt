package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.extension.kind.UnsupportedSdk
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Which library version the extension was built with and which ones this app loads instead. */
@Composable
internal fun unsupportedSdkMessage(extensionName: String, sdk: UnsupportedSdk): String {
    return stringResource(
        MR.strings.ext_unsupported_sdk_message,
        extensionName,
        sdk.family.library,
        sdk.libVersionName,
        stringResource(sdk.family.titleRes),
        sdk.family.versionsText,
    )
}

/**
 * What would let it load: a newer app for a newer library, or else an update of an [installed] extension. A store's
 * listing of an older one has nothing to update to.
 */
@Composable
internal fun unsupportedSdkAdvice(sdk: UnsupportedSdk, installed: Boolean): String? {
    return when {
        sdk.isNewer == true -> stringResource(MR.strings.ext_unsupported_sdk_update_app)
        sdk.isNewer == false && installed -> stringResource(MR.strings.ext_unsupported_sdk_update_extension)
        else -> null
    }
}
