package mihon.translation.runtime.system

import android.app.Application
import android.os.Build
import mihon.translation.spi.TranslationEngineContribution

internal fun createAndroidSystemTranslationContribution(
    application: Application,
): TranslationEngineContribution {
    val engine = AndroidSystemTranslationEngine(
        DefaultAndroidSystemTranslationPlatform(
            sdkInt = Build.VERSION.SDK_INT,
            bridge = createAndroidTranslationManagerBridge(application),
        ),
    )
    return TranslationEngineContribution(
        engine = engine,
        setup = engine,
        order = ANDROID_SYSTEM_ENGINE_ORDER,
    )
}

private const val ANDROID_SYSTEM_ENGINE_ORDER = -1_000
