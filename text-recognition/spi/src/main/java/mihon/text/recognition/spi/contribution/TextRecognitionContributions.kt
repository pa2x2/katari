package mihon.text.recognition.spi.contribution

import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.component.TextRecognitionBuildAvailability
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.spi.component.TextRecognitionComponent

/**
 * One independently owned component contribution. A contribution without [component] keeps a component visible in
 * the catalog of a build that intentionally excludes it.
 */
data class TextRecognitionComponentContribution(
    val catalogEntry: KnownTextRecognitionComponent,
    val component: TextRecognitionComponent? = null,
    val order: Int = DEFAULT_ORDER,
) {
    init {
        require(component == null || component.catalogEntry == catalogEntry) {
            "Text recognition contribution catalog does not match its component"
        }
        require((component != null) == (catalogEntry.buildAvailability == TextRecognitionBuildAvailability.Included)) {
            "Text recognition component ${catalogEntry.id.value} build availability does not match its contribution"
        }
    }

    constructor(
        component: TextRecognitionComponent,
        order: Int = DEFAULT_ORDER,
    ) : this(catalogEntry = component.catalogEntry, component = component, order = order)

    private companion object {
        const val DEFAULT_ORDER = 0
    }
}

data class TextRecognitionPresetContribution(
    val preset: TextRecognitionPreset,
    val order: Int = 0,
)
