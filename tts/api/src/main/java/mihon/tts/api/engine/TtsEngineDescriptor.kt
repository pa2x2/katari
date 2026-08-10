package mihon.tts.api.engine

import androidx.annotation.DrawableRes

data class KnownTtsEngine(
    val id: TtsEngineId,
    val providerId: TtsProviderId,
    val providerName: String,
    val engineName: String,
    val buildAvailability: TtsEngineBuildAvailability,
    val artwork: TtsEngineArtwork,
    val details: TtsEngineDetails,
    val documentationUrl: String? = null,
) {
    init {
        require(providerName.isNotBlank())
        require(engineName.isNotBlank())
        require(documentationUrl == null || documentationUrl.isNotBlank())
    }
}

sealed interface TtsEngineArtwork {
    data class Bundled(
        @DrawableRes val resourceId: Int,
    ) : TtsEngineArtwork {
        init {
            require(resourceId != 0)
        }
    }

    data class InstalledApplication(
        val packageName: String,
        @DrawableRes val fallbackResourceId: Int,
    ) : TtsEngineArtwork {
        init {
            require(packageName.isNotBlank())
            require(fallbackResourceId != 0)
        }
    }
}

data class TtsEngineDetails(
    val description: String,
    val processingDescription: String,
    val privacyDescription: String,
) {
    init {
        require(description.isNotBlank())
        require(processingDescription.isNotBlank())
        require(privacyDescription.isNotBlank())
    }
}

sealed interface TtsEngineBuildAvailability {
    data object Included : TtsEngineBuildAvailability

    data class NotIncluded(
        val reason: String,
    ) : TtsEngineBuildAvailability {
        init {
            require(reason.isNotBlank())
        }
    }
}
