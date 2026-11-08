package app.katari.extension.runtime.jvm

import eu.kanade.tachiyomi.extension.kind.ExtensionFamily
import eu.kanade.tachiyomi.extension.platform.HostedExtensionFamilies

/**
 * The desktop host runs LNReader plugins, which need no apk, and the apks it converts: those of Katari's Entry SDK, the
 * Tachiyomi extensions-lib and Aniyomi's. Aniyomi's sources browse and list episodes; desktop has no player to open an
 * episode in yet.
 */
class DesktopHostedExtensionFamilies : HostedExtensionFamilies {
    override val families: Set<ExtensionFamily> = setOf(
        ExtensionFamily.LNREADER,
        ExtensionFamily.KATARI,
        ExtensionFamily.TACHIYOMI,
        ExtensionFamily.ANIYOMI,
    )
}
