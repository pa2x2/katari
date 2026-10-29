package mihon.extension.runtime.jvm

import eu.kanade.tachiyomi.extension.kind.ExtensionFamily
import eu.kanade.tachiyomi.extension.platform.HostedExtensionFamilies

/** The desktop host runs LNReader plugins, which need no apk; it reads no apks yet. */
class DesktopHostedExtensionFamilies : HostedExtensionFamilies {
    override val families: Set<ExtensionFamily> = setOf(ExtensionFamily.LNREADER)
}
