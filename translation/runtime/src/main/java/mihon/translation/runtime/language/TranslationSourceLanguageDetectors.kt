package mihon.translation.runtime.language

import android.app.Application
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponents
import mihon.feature.runtime.application.instances
import mihon.translation.runtime.component.TranslationRuntimeComponent
import mihon.translation.runtime.component.TranslationRuntimeContribution
import mihon.translation.spi.language.TranslationSourceLanguageDetector

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
