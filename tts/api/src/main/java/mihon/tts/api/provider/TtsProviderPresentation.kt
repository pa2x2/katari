package mihon.tts.api.provider

import mihon.tts.api.engine.TtsProviderId

data class TtsProviderPresentation(
    val providerId: TtsProviderId,
    val providerName: String,
    val engineName: String,
    val disclosure: TtsProviderDisclosure? = null,
    val documentationUrl: String? = null,
) {
    init {
        require(providerName.isNotBlank())
        require(engineName.isNotBlank())
        require(documentationUrl == null || documentationUrl.isNotBlank())
    }
}

data class TtsProviderDisclosure(
    val title: String,
    val message: String,
    val confirmationLabel: String,
    val documentationUrl: String? = null,
) {
    init {
        require(title.isNotBlank())
        require(message.isNotBlank())
        require(confirmationLabel.isNotBlank())
        require(documentationUrl == null || documentationUrl.isNotBlank())
    }
}
