package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.storage.DesktopPreferenceFiles
import tachiyomi.core.common.preference.PlatformPreferenceFiles

@BindingContainer
interface PreferenceBindings {
    @Binds
    val DesktopPreferenceFiles.preferenceFiles: PlatformPreferenceFiles
}
