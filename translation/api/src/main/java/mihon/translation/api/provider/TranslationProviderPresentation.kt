package mihon.translation.api.provider

import mihon.translation.api.engine.TranslationProviderId

data class TranslationProviderPresentation(
    val providerId: TranslationProviderId,
    val providerName: String,
    val engineName: String,
    val invocationPolicy: TranslationInvocationPolicy,
    val outputMode: TranslationProviderOutputMode = TranslationProviderOutputMode.InlineResult,
    val disclosure: TranslationProviderDisclosure? = null,
    val resultAttribution: TranslationResultAttribution? = null,
    val documentationUrl: String? = null,
) {
    init {
        require(providerName.isNotBlank())
        require(engineName.isNotBlank())
    }
}

enum class TranslationProviderOutputMode {
    InlineResult,
    ProviderSurface,
}

sealed interface TranslationInvocationPolicy {
    data object Immediate : TranslationInvocationPolicy

    data class ExplicitAction(
        val label: String,
    ) : TranslationInvocationPolicy {
        init {
            require(label.isNotBlank())
        }
    }
}

data class TranslationProviderDisclosure(
    val title: String,
    val message: String,
    val confirmationLabel: String,
    val documentationUrl: String? = null,
) {
    init {
        require(title.isNotBlank())
        require(message.isNotBlank())
        require(confirmationLabel.isNotBlank())
    }
}

data class TranslationResultAttribution(
    val label: String,
) {
    init {
        require(label.isNotBlank())
    }
}
