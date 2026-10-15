package mihon.entry.interactions.reader.settings

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.core.metro.BasePreferenceStore
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationSettingsProvider
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsProvider
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsRegistry
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
import tachiyomi.core.common.preference.PreferenceStore

@ContributesTo(AppScope::class)
@BindingContainer
object ReaderSettingsBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun basePreferences(@BasePreferenceStore preferenceStore: PreferenceStore): ReaderBasePreferences =
        ReaderBasePreferences(preferenceStore)

    // Chapter preparation comes first so the Readers root lists its setting before type-specific ones.
    @Provides
    @SingleIn(AppScope::class)
    fun sharedSettingsRegistry(
        chapterPreparation: ReaderChapterPreparationSettingsProvider,
        typeProviders: Set<ReaderSharedSettingsProvider>,
    ): ReaderSharedSettingsRegistry = ReaderSharedSettingsRegistry(listOf(chapterPreparation) + typeProviders)

    @Provides
    @IntoSet
    fun exportBasePreferences(preferences: () -> ReaderBasePreferences): InjektExport<*> = injektExport(preferences)

    @Provides
    @IntoSet
    fun exportSharedSettingsRegistry(registry: () -> ReaderSharedSettingsRegistry): InjektExport<*> =
        injektExport(registry)
}
