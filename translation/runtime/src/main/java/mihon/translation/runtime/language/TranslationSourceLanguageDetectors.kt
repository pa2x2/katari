package mihon.translation.runtime

import android.app.Application
import mihon.feature.runtime.ApplicationFeatureRuntimeComponents
import mihon.feature.runtime.instances
import mihon.translation.spi.TranslationSourceLanguageDetector

internal fun createTranslationSourceLanguageDetectors(
    application: Application,
    components: ApplicationFeatureRuntimeComponents,
): List<TranslationSourceLanguageDetector> {
    val detectors = buildList {
        androidTextClassifierSourceLanguageDetector(application)?.let(::add)
        components.instances<TranslationRuntimeComponent>()
            .flatMapTo(this) { component -> component.contribute(application).sourceLanguageDetectors }
    }
    val duplicateIds = detectors
        .groupBy(TranslationSourceLanguageDetector::id)
        .filterValues { it.size > 1 }
    check(duplicateIds.isEmpty()) {
        "Translation source language detectors have duplicate ids: ${duplicateIds.keys.map { it.value }.sorted()}"
    }
    return detectors
}
