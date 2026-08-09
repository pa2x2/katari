package mihon.translation.runtime.component

import android.app.Application
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponents
import mihon.feature.runtime.application.instances

internal fun createTranslationRuntimeContributions(
    application: Application,
    components: ApplicationFeatureRuntimeComponents,
): List<TranslationRuntimeContribution> {
    return components.instances<TranslationRuntimeComponent>()
        .map { component -> component.contribute(application) }
}
