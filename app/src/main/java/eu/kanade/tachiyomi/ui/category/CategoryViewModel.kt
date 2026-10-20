package eu.kanade.tachiyomi.ui.category

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.icerock.moko.resources.StringResource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.data.library.LibraryUpdateSchedule
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
import mihon.app.di.AppUiScope
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.DeleteCategory
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.RenameCategory
import tachiyomi.domain.category.interactor.ReorderCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.update.model.CategoryUpdateRules
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import tachiyomi.i18n.*
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class CategoryViewModel(
    private val getCategories: GetCategories,
    private val createCategoryWithName: CreateCategoryWithName,
    private val deleteCategory: DeleteCategory,
    private val reorderCategory: ReorderCategory,
    private val renameCategory: RenameCategory,
    private val rulesRepository: LibraryUpdateRulesRepository,
    private val application: Application,
) : ViewModel() {

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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), CategoryScreenState.Loading)

    fun createCategory(name: String) {
        viewModelScope.launch {
            when (createCategoryWithName.await(name)) {
                is CreateCategoryWithName.Result.InternalError -> _events.send(CategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            when (deleteCategory.await(categoryId = categoryId)) {
                is DeleteCategory.Result.InternalError -> _events.send(CategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun changeOrder(category: Category, newIndex: Int) {
        viewModelScope.launch {
            when (reorderCategory.await(category, newIndex)) {
                is ReorderCategory.Result.InternalError -> _events.send(CategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun showEditDialog(category: Category) {
        viewModelScope.launch {
            val rules = rulesRepository.getCategoryRules()[category.id] ?: CategoryUpdateRules(category.id)
            showDialog(CategoryDialog.Edit(category, rules))
        }
    }

    fun editCategory(category: Category, rules: CategoryUpdateRules, name: String, autoUpdate: Boolean) {
        viewModelScope.launch {
            if (name != category.name) {
                when (renameCategory.await(category, name)) {
                    is RenameCategory.Result.InternalError -> _events.send(CategoryEvent.InternalError)
                    else -> {}
                }
            }
            if (autoUpdate != rules.autoUpdate) {
                rulesRepository.setCategoryRules(rules.copy(autoUpdate = autoUpdate))
                // Automatic updates run as often as the most frequent category that is checked asks.
                LibraryUpdateSchedule.scheduleActiveProfile(application)
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
