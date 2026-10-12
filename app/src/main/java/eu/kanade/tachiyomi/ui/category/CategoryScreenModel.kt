package eu.kanade.tachiyomi.ui.category

import android.app.Application
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.DeleteCategory
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.RenameCategory
import tachiyomi.domain.category.interactor.ReorderCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.update.model.CategoryUpdateRules
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import tachiyomi.i18n.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

class CategoryScreenModel(
    private val getCategories: GetCategories = Injekt.get(),
    private val createCategoryWithName: CreateCategoryWithName = Injekt.get(),
    private val deleteCategory: DeleteCategory = Injekt.get(),
    private val reorderCategory: ReorderCategory = Injekt.get(),
    private val renameCategory: RenameCategory = Injekt.get(),
    private val rulesRepository: LibraryUpdateRulesRepository = Injekt.get(),
    private val application: Application = Injekt.get(),
) : ScreenModel {

    private val _events: Channel<CategoryEvent> = Channel()
    val events = _events.receiveAsFlow()

    private val dialog = MutableStateFlow<CategoryDialog?>(null)

    val state: StateFlow<CategoryScreenState> = combine(
        getCategories.subscribe(),
        dialog,
    ) { categories, dialog ->
        CategoryScreenState.Success(
            categories = categories.filterNot(Category::isSystemCategory),
            dialog = dialog,
        )
    }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5.seconds), CategoryScreenState.Loading)

    fun createCategory(name: String) {
        screenModelScope.launch {
            when (createCategoryWithName.await(name)) {
                is CreateCategoryWithName.Result.InternalError -> _events.send(CategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun deleteCategory(categoryId: Long) {
        screenModelScope.launch {
            when (deleteCategory.await(categoryId = categoryId)) {
                is DeleteCategory.Result.InternalError -> _events.send(CategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun changeOrder(category: Category, newIndex: Int) {
        screenModelScope.launch {
            when (reorderCategory.await(category, newIndex)) {
                is ReorderCategory.Result.InternalError -> _events.send(CategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun showEditDialog(category: Category) {
        screenModelScope.launch {
            val rules = rulesRepository.getCategoryRules()[category.id] ?: CategoryUpdateRules(category.id)
            showDialog(CategoryDialog.Edit(category, rules))
        }
    }

    fun editCategory(category: Category, rules: CategoryUpdateRules, name: String, autoUpdate: Boolean) {
        screenModelScope.launch {
            if (name != category.name) {
                when (renameCategory.await(category, name)) {
                    is RenameCategory.Result.InternalError -> _events.send(CategoryEvent.InternalError)
                    else -> {}
                }
            }
            if (autoUpdate != rules.autoUpdate) {
                rulesRepository.setCategoryRules(rules.copy(autoUpdate = autoUpdate))
                // Automatic updates run as often as the most frequent category that is checked asks.
                LibraryUpdateJob.setupTask(application)
            }
        }
    }

    fun showDialog(dialog: CategoryDialog) {
        this.dialog.update { dialog }
    }

    fun dismissDialog() {
        dialog.update { null }
    }
}

sealed interface CategoryDialog {
    data object Create : CategoryDialog
    data class Edit(val category: Category, val rules: CategoryUpdateRules) : CategoryDialog
    data class Delete(val category: Category) : CategoryDialog
}

sealed interface CategoryEvent {
    sealed class LocalizedMessage(val stringRes: StringResource) : CategoryEvent
    data object InternalError : LocalizedMessage(MR.strings.internal_error)
}

sealed interface CategoryScreenState {

    @Immutable
    data object Loading : CategoryScreenState

    @Immutable
    data class Success(
        val categories: List<Category>,
        val dialog: CategoryDialog? = null,
    ) : CategoryScreenState {

        val isEmpty: Boolean
            get() = categories.isEmpty()
    }
}
