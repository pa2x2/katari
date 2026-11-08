package app.katari.desktop.di

import app.katari.desktop.storage.DesktopPreferenceFiles
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import tachiyomi.core.common.preference.PlatformPreferenceFiles

@BindingContainer
interface PreferenceBindings {
    @Binds
    val DesktopPreferenceFiles.preferenceFiles: PlatformPreferenceFiles
}
