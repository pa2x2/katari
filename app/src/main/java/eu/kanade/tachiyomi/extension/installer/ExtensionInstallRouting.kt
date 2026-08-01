package eu.kanade.tachiyomi.extension.installer

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.InstallRoute
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.UserActionBehavior

internal sealed interface ExtensionInstallOperation {
    data object NewInstall : ExtensionInstallOperation
    data class ManualUpdate(val extension: Extension.Installed) : ExtensionInstallOperation
    data class AutomaticUpdate(val extension: Extension.Installed) : ExtensionInstallOperation
}

internal data class ExtensionInstallRouting(
    val installRoute: InstallRoute,
    val userActionBehavior: UserActionBehavior,
)

internal fun ExtensionInstallOperation.routing(): ExtensionInstallRouting {
    val extension = when (this) {
        ExtensionInstallOperation.NewInstall -> null
        is ExtensionInstallOperation.ManualUpdate -> extension
        is ExtensionInstallOperation.AutomaticUpdate -> extension
    }
    val installRoute = if (extension?.isShared == false) InstallRoute.Private else InstallRoute.Configured
    val userActionBehavior = when (this) {
        is ExtensionInstallOperation.AutomaticUpdate -> UserActionBehavior.MarkAsRequiresUserAction
        ExtensionInstallOperation.NewInstall,
        is ExtensionInstallOperation.ManualUpdate,
        -> UserActionBehavior.LaunchPrompt
    }

    return ExtensionInstallRouting(installRoute, userActionBehavior)
}
