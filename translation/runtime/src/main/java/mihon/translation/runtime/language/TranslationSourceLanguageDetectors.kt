package mihon.translation.runtime

import android.app.Application
import mihon.feature.runtime.ApplicationFeatureRuntimeComponents
import mihon.feature.runtime.instances
import mihon.translation.spi.TranslationSourceLanguageDetector

internal fun createTranslationSourceLanguageDetectors(
    application: Application,
    contributions: List<TranslationRuntimeContribution>,
): List<TranslationSourceLanguageDetector> {
    val detectors = buildList {
        androidTextClassifierSourceLanguageDetector(application)?.let(::add)
        contributions.flatMapTo(this, TranslationRuntimeContribution::sourceLanguageDetectors)
    }
    val duplicateIds = detectors
        .groupBy(TranslationSourceLanguageDetector::id)
        .filterValues { it.size > 1 }
    check(duplicateIds.isEmpty()) {
        "Translation source language detectors have duplicate ids: ${duplicateIds.keys.map { it.value }.sorted()}"
    }
    return detectors
}

internal fun createTranslationRuntimeContributions(
    application: Application,
    components: ApplicationFeatureRuntimeComponents,
): List<TranslationRuntimeContribution> {
    return components.instances<TranslationRuntimeComponent>()
        .map { component -> component.contribute(application) }
}
