package mihon.translation.spi

import kotlinx.coroutines.flow.Flow
import mihon.translation.api.TranslationModelId
import mihon.translation.api.TranslationModelInventoryItem
import mihon.translation.api.TranslationModelOperationResult
import mihon.translation.api.TranslationOperationProgress

interface TranslationModelManager {
    suspend fun inventory(): List<TranslationModelInventoryItem>

    fun download(
        models: Set<TranslationModelId>,
        policy: TranslationModelDownloadPolicy,
    ): Flow<TranslationModelOperation>

    suspend fun delete(models: Set<TranslationModelId>): TranslationModelOperationResult
}

data class TranslationModelDownloadPolicy(
    val requireUnmeteredNetwork: Boolean,
)

sealed interface TranslationModelOperation {
    data class Progress(
        val progress: TranslationOperationProgress,
    ) : TranslationModelOperation

    data class Finished(
        val result: TranslationModelOperationResult,
    ) : TranslationModelOperation
}
