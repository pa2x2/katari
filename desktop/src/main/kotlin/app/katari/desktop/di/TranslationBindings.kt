package app.katari.desktop.di

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.desktop.secrets.SecretServiceStore
import app.katari.desktop.translation.InProcessEntryTranslateWorkController
import app.katari.desktop.translation.WindowEntryTranslateSettingsScreens
import app.katari.desktop.translation.server.FileServerConnectionPreferenceStores
import app.katari.desktop.translation.server.UnguardedServerLocalNetwork
import app.katari.desktop.translation.server.WindowServerSetupDestination
import app.katari.desktop.work.ResumableWork
import app.katari.entry.interactions.translate.work.EntryTranslateWorkController
import app.katari.translation.api.host.TranslationSettingsDestination
import app.katari.translation.provider.server.SecretStore
import app.katari.translation.provider.server.ServerConnectionPreferenceStores
import app.katari.translation.provider.server.ServerLocalNetworkGuard
import app.katari.translation.provider.server.setup.ServerSetupDestination
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.entry.translation.EntryTranslateSettingsScreens

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
