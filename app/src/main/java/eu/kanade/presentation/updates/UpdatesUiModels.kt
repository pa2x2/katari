package eu.kanade.presentation.updates

import eu.kanade.core.util.insertSeparators
import java.time.LocalDate

sealed interface UpdatesUiModel<out T> {
    data class Header(val date: LocalDate) : UpdatesUiModel<Nothing>

    data class Item<T>(val item: T) : UpdatesUiModel<T>
}

fun <T> List<T>.toUpdatesUiModels(
    dateProvider: (T) -> LocalDate,
): List<UpdatesUiModel<T>> {
    return map { UpdatesUiModel.Item(it) }
        .insertSeparators { before, after ->
            val beforeDate = before?.item?.let(dateProvider)
            val afterDate = after?.item?.let(dateProvider)
            when {
                beforeDate != afterDate && afterDate != null -> UpdatesUiModel.Header(afterDate)
                else -> null
            }
        }
}
