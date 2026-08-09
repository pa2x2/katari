package mihon.tts.runtime.component

import android.app.Application
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponents
import mihon.feature.runtime.application.instances

internal fun createTtsRuntimeContributions(
    application: Application,
    components: ApplicationFeatureRuntimeComponents,
): List<TtsRuntimeContribution> {
    return components.instances<TtsRuntimeComponent>()
        .map { component -> component.contribute(application) }
}
