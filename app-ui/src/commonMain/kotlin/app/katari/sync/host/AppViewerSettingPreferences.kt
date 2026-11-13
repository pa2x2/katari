package app.katari.sync.host

import app.katari.entry.interactions.media.EntryViewerSettingsFeature
import app.katari.profiles.core.ProfileStore
import app.katari.sync.client.part.ViewerSettingPreferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * The settings of the viewers this platform has, by the per-title value each one has: a title's value stands in for its
 * definition's profile preference. Read on first use, since sync drains its outbox long before a viewer opens.
 */
@Inject
@ContributesBinding(AppScope::class)
class AppViewerSettingPreferences(
    private val viewerSettings: () -> EntryViewerSettingsFeature,
    private val profileStore: ProfileStore,
) : ViewerSettingPreferences {
    private val keys by lazy {
        viewerSettings().destinations
            .flatMap { it.settings.settings }
            .associate { (it.id.providerId to it.id.key) to profileStore.preferenceKey(it.profilePreference) }
    }

    override fun preferenceKey(providerId: String, settingKey: String): String? =
        keys[providerId to settingKey]
}
