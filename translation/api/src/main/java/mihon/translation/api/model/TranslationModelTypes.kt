package mihon.translation.api.model

import mihon.language.api.tag.LanguageTag

@JvmInline
value class TranslationModelId(
    val value: String,
) {
    init {
        require(value.isNotBlank())
    }
}

data class TranslationModelDescriptor(
    val id: TranslationModelId,
    val language: LanguageTag,
    val displayName: String,
    val approximateSizeBytes: Long? = null,
) {
    init {
        require(displayName.isNotBlank())
        require(approximateSizeBytes == null || approximateSizeBytes > 0)
    }
}

data class TranslationOperationProgress(
    val completed: Long,
    val total: Long?,
) {
    init {
        require(completed >= 0)
        require(total == null || total >= completed)
    }
}

sealed interface TranslationModelOperationResult {
    data object Completed : TranslationModelOperationResult

    data class Failed(
        val reason: String,
    ) : TranslationModelOperationResult {
        init {
            require(reason.isNotBlank())
        }
    }
}
