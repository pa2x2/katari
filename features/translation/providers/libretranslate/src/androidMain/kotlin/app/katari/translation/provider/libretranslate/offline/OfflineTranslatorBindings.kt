package app.katari.translation.provider.libretranslate.offline

import android.app.Application
import app.katari.translation.provider.libretranslate.protocol.LibreTranslateHttpClient
import app.katari.translation.spi.contribution.TranslationEngineContribution
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
object OfflineTranslatorBindings {
    @Provides
    @IntoSet
    fun engine(application: Application): TranslationEngineContribution {
        val settings = OfflineTranslatorConfiguration(application)
        val providerApplication = OfflineTranslatorApplication(application)
        val engine = OfflineTranslatorEngine(
            application = providerApplication,
            settings = settings,
            serviceFactory = { endpoint ->
                LibreTranslateHttpClient(
                    httpClient = OfflineTranslatorNetwork.httpClient,
                    endpoint = endpoint,
                )
            },
        )
        return TranslationEngineContribution(
            engine = engine,
            setup = OfflineTranslatorSetup(
                context = application,
                application = providerApplication,
                settings = settings,
            ),
            order = OFFLINE_TRANSLATOR_ORDER,
        )
    }
}

private const val OFFLINE_TRANSLATOR_ORDER = 100
