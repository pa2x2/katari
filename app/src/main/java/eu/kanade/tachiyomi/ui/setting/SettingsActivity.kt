package eu.kanade.tachiyomi.ui.setting

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import app.katari.entry.viewer.settings.navigation.ViewerSettingsNavigation
import app.katari.language.api.tag.LanguageTag
import app.katari.text.recognition.api.host.TextRecognitionSettingsNavigation
import app.katari.translation.api.host.TranslationSettingsNavigation
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.more.AppMigratingScreen
import eu.kanade.presentation.more.settings.screen.textrecognition.languages.TextRecognitionOverridePipelineScreen
import eu.kanade.presentation.util.DefaultNavigatorScreenTransition
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.base.migration.rememberAppMigrating
import eu.kanade.tachiyomi.util.view.setComposeContent

class SettingsActivity : BaseActivity() {

    init {
        registerSecureActivity(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val screen = when (intent.action) {
            TranslationSettingsNavigation.ACTION_OPEN_SETTINGS ->
                SettingsScreen(SettingsScreen.Destination.Translation)
            TextRecognitionSettingsNavigation.ACTION_CHOOSE_PIPELINE -> {
                val language = intent.getStringExtra(TextRecognitionSettingsNavigation.EXTRA_LANGUAGE)
                    ?.let(LanguageTag::parse)
                if (language == null) {
                    finish()
                    return
                }
                TextRecognitionOverridePipelineScreen(language.value, storeOnConfirm = true)
            }
            ViewerSettingsNavigation.ACTION_OPEN_SETTINGS ->
                SettingsScreen(
                    destination = SettingsScreen.Destination.Readers,
                    viewerSettingsSurfaceId = intent.getStringExtra(
                        ViewerSettingsNavigation.EXTRA_SURFACE_ID,
                    ),
                    viewerSettingsHighlight = intent.getStringExtra(ViewerSettingsNavigation.EXTRA_HIGHLIGHT),
                )
            else -> {
                finish()
                return
            }
        }

        setComposeContent {
            if (rememberAppMigrating()) {
                AppMigratingScreen()
                return@setComposeContent
            }

            Navigator(screen = screen) { navigator ->
                DefaultNavigatorScreenTransition(navigator)
            }
        }
    }
}
