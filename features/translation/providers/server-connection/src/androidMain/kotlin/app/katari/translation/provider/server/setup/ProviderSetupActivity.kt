package app.katari.translation.provider.server.setup

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import app.katari.core.metro.metroGraph
import app.katari.presentation.kit.theme.KatariTheme
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/** Android's shell around a provider's setup [screen]: the activity shows it alone and closes when the user is done. */
abstract class ProviderSetupActivity(private val screen: ProviderSetupScreen) : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModelFactory = metroGraph<ViewModelGraph>().metroViewModelFactory
        setContent {
            CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) {
                KatariTheme {
                    screen.Alone(onClose = ::finish)
                }
            }
        }
    }
}
