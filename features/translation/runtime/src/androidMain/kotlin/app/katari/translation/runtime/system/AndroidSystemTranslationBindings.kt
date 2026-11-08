package app.katari.translation.runtime.system

import android.app.Application
import app.katari.translation.runtime.preference.DeviceDefaultTranslationEngine
import app.katari.translation.spi.contribution.TranslationEngineContribution
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

/** Android's own translation service, the engine translation uses until the user picks another. */
@ContributesTo(AppScope::class)
@BindingContainer
object AndroidSystemTranslationBindings {
    @Provides
    @IntoSet
    fun systemEngine(application: Application): TranslationEngineContribution =
        createAndroidSystemTranslationContribution(application)

    @Provides
    @IntoSet
    fun deviceDefaultEngine(): DeviceDefaultTranslationEngine =
        DeviceDefaultTranslationEngine(AndroidSystemTranslationEngine.ENGINE_ID)
}
