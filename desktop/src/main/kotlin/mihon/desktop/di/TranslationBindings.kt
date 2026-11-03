package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.entry.translation.EntryTranslateSettingsScreens
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.desktop.secrets.SecretServiceStore
import mihon.desktop.translation.InProcessEntryTranslateWorkController
import mihon.desktop.translation.WindowEntryTranslateSettingsScreens
import mihon.desktop.translation.server.FileServerConnectionPreferenceStores
import mihon.desktop.translation.server.UnguardedServerLocalNetwork
import mihon.desktop.translation.server.WindowServerSetupDestination
import mihon.desktop.work.ResumableWork
import mihon.entry.interactions.translate.work.EntryTranslateWorkController
import mihon.translation.api.host.TranslationSettingsDestination
import mihon.translation.provider.server.SecretStore
import mihon.translation.provider.server.ServerConnectionPreferenceStores
import mihon.translation.provider.server.ServerLocalNetworkGuard
import mihon.translation.provider.server.setup.ServerSetupDestination

@BindingContainer
interface TranslationBindings {
    @Binds
    val InProcessEntryTranslateWorkController.entryTranslateWorkController: EntryTranslateWorkController

    @Binds
    @IntoSet
    val InProcessEntryTranslateWorkController.resumableEntryTranslations: ResumableWork

    @Binds
    val WindowEntryTranslateSettingsScreens.entryTranslateSettingsScreens: EntryTranslateSettingsScreens

    @Binds
    val WindowEntryTranslateSettingsScreens.translationSettingsDestination: TranslationSettingsDestination

    @Binds
    val FileServerConnectionPreferenceStores.serverConnectionPreferenceStores: ServerConnectionPreferenceStores

    @Binds
    val UnguardedServerLocalNetwork.serverLocalNetworkGuard: ServerLocalNetworkGuard

    @Binds
    val WindowServerSetupDestination.serverSetupDestination: ServerSetupDestination

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun secretStore(operatingSystem: DesktopOperatingSystem): SecretStore = when (operatingSystem) {
            DesktopOperatingSystem.LINUX -> SecretServiceStore()
        }
    }
}
