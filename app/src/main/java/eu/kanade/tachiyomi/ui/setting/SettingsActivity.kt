package eu.kanade.tachiyomi.ui.setting

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.util.DefaultNavigatorScreenTransition
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.view.setComposeContent
import mihon.core.migration.Migrator
import mihon.entry.viewer.settings.navigation.ViewerSettingsNavigation
import mihon.translation.api.host.TranslationSettingsNavigation

class SettingsActivity : BaseActivity() {

    init {
        registerSecureActivity(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Migrator.awaitAndRelease()
        enableEdgeToEdge()

        val screen = when (intent.action) {
            TranslationSettingsNavigation.ACTION_OPEN_SETTINGS ->
                SettingsScreen(SettingsScreen.Destination.Translation)
            ViewerSettingsNavigation.ACTION_OPEN_SETTINGS ->
                SettingsScreen(
                    destination = SettingsScreen.Destination.Readers,
                    viewerSettingsSurfaceId = intent.getStringExtra(
                        ViewerSettingsNavigation.EXTRA_SURFACE_ID,
                    ),
                )
            else -> {
                finish()
                return
            }
        }

        setComposeContent {
            Navigator(screen = screen) { navigator ->
                DefaultNavigatorScreenTransition(navigator)
            }
        }
    }
}
