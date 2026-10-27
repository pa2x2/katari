package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import mihon.desktop.storage.DesktopDirectories

@BindingContainer
object NetworkBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun networkHelper(directories: DesktopDirectories, preferences: NetworkPreferences): NetworkHelper =
        NetworkHelper(cacheDirectory = directories.cache.toFile(), preferences = preferences)
}
