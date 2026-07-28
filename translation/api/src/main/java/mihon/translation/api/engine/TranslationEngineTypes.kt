package mihon.translation.api

import androidx.annotation.DrawableRes

@JvmInline
value class TranslationEngineId(
    val value: String,
) {
    init {
        require(ID_PATTERN.matches(value)) { "Invalid Translation engine id: '$value'" }
    }

    private companion object {
        val ID_PATTERN = Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*""")
    }
}

@JvmInline
value class TranslationProviderId(
    val value: String,
) {
    init {
        require(ID_PATTERN.matches(value)) { "Invalid Translation provider id: '$value'" }
    }

    private companion object {
        val ID_PATTERN = Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*""")
    }
}

sealed interface TranslationEngineSelection {
    data object ProfileDefault : TranslationEngineSelection

    data class Explicit(
        val engine: TranslationEngineId,
    ) : TranslationEngineSelection
}

data class KnownTranslationEngine(
    val id: TranslationEngineId,
    val providerId: TranslationProviderId,
    val providerName: String,
    val engineName: String,
    val buildAvailability: TranslationEngineBuildAvailability,
    val artwork: TranslationEngineArtwork,
    val details: TranslationEngineDetails,
    val documentationUrl: String? = null,
) {
    init {
        require(providerName.isNotBlank())
        require(engineName.isNotBlank())
        require(documentationUrl == null || documentationUrl.isNotBlank())
    }
}

sealed interface TranslationEngineArtwork {
    data class Bundled(
        @DrawableRes val resourceId: Int,
    ) : TranslationEngineArtwork {
        init {
            require(resourceId != 0)
        }
    }

    data class InstalledApplication(
        val packageName: String,
        @DrawableRes val fallbackResourceId: Int,
    ) : TranslationEngineArtwork {
        init {
            require(packageName.isNotBlank())
            require(fallbackResourceId != 0)
        }
    }
}

data class TranslationEngineDetails(
    val description: String,
    val processingLocation: String,
    val privacyDescription: String,
    val artworkAttribution: String? = null,
    val artworkAttributionUrl: String? = null,
) {
    init {
        require(description.isNotBlank())
        require(processingLocation.isNotBlank())
        require(privacyDescription.isNotBlank())
        require(artworkAttribution == null || artworkAttribution.isNotBlank())
        require(artworkAttributionUrl == null || artworkAttributionUrl.isNotBlank())
        require(artworkAttributionUrl == null || artworkAttribution != null)
    }
}

data class TranslationEngineState(
    val engine: KnownTranslationEngine,
    val presentation: TranslationProviderPresentation?,
    val status: TranslationEngineStatus,
    val action: TranslationEngineAction? = null,
)

sealed interface TranslationEngineStatus {
    data object Checking : TranslationEngineStatus

    data object Ready : TranslationEngineStatus

    data object NotInstalled : TranslationEngineStatus

    data class ConfigurationRequired(
        val reason: String,
    ) : TranslationEngineStatus {
        init {
            require(reason.isNotBlank())
        }
    }

    data class ProviderDisclosureRequired(
        val disclosure: TranslationProviderDisclosure,
    ) : TranslationEngineStatus

    data class ModelDownloadRequired(
        val models: List<TranslationModelDescriptor>,
    ) : TranslationEngineStatus {
        init {
            require(models.isNotEmpty())
        }
    }

    data class SystemSetupRequired(
        val reason: TranslationSystemSetupReason,
    ) : TranslationEngineStatus

    data class SetupInProgress(
        val progress: TranslationOperationProgress? = null,
    ) : TranslationEngineStatus

    data class Unavailable(
        val reason: TranslationUnavailableReason,
    ) : TranslationEngineStatus
}

enum class TranslationEngineAction {
    Install,
    Configure,
    Setup,
}

sealed interface TranslationEngineBuildAvailability {
    data object Included : TranslationEngineBuildAvailability

    data class NotIncluded(
        val reason: String,
    ) : TranslationEngineBuildAvailability {
        init {
            require(reason.isNotBlank())
        }
    }
}
