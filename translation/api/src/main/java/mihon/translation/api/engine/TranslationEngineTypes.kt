package mihon.translation.api

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
    val documentationUrl: String? = null,
)

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
