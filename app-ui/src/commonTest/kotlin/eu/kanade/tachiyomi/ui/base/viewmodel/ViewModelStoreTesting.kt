package eu.kanade.tachiyomi.ui.base.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Creates [VM] in this store the way a screen's `viewModel { }` call does. Clearing the store afterwards runs the
 * model's `onCleared` and closeables and cancels its `viewModelScope`, as leaving the screen does.
 */
inline fun <reified VM : ViewModel> ViewModelStore.createViewModel(noinline factory: () -> VM): VM =
    ViewModelProvider.create(this, viewModelFactory { initializer { factory() } })[VM::class]
