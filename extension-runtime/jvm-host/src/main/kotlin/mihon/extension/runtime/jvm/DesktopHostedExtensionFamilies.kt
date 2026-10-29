package mihon.extension.runtime.jvm

import eu.kanade.tachiyomi.extension.kind.ExtensionFamily
import eu.kanade.tachiyomi.extension.platform.HostedExtensionFamilies

/**
 * The desktop host runs LNReader plugins, which need no apk, and the apks of Katari's Entry SDK and of the Tachiyomi
 * extensions-lib, which it converts. Aniyomi's apks aren't hosted yet.
 */
class DesktopHostedExtensionFamilies : HostedExtensionFamilies {
    override val families: Set<ExtensionFamily> = setOf(
        ExtensionFamily.LNREADER,
        ExtensionFamily.KATARI,
        ExtensionFamily.TACHIYOMI,
    )
}
