package eu.kanade.tachiyomi.ui.base.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import cafe.adriel.voyager.core.lifecycle.NavigatorScreenLifecycleProvider
import cafe.adriel.voyager.core.lifecycle.ScreenLifecycleContentProvider
import cafe.adriel.voyager.core.lifecycle.ScreenLifecycleOwner
import cafe.adriel.voyager.core.lifecycle.ScreenLifecycleStore
import cafe.adriel.voyager.core.screen.Screen

/**
 * What Voyager's Android screen lifecycle owner gives every screen, which Voyager has no desktop counterpart for: its
 * own ViewModel store, lifecycle and saved-state registry. Without it, every screen reads the window's, so two screens
 * of one class, such as two sources' catalogues, share one ViewModel.
 *
 * The owner is kept in the screen's [ScreenLifecycleStore] entry and cleared and destroyed when the screen leaves the
 * navigator. While the screen is shown its lifecycle follows the enclosing one (the window's, or the screen hosting its
 * navigator), up to resumed; while another screen covers it, it is created. Saved state is kept only as long as the
 * owner: a desktop window is never recreated from saved state.
 */
class DesktopScreenLifecycleOwner private constructor() :
    ScreenLifecycleOwner,
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    HasDefaultViewModelProviderFactory {
    override val lifecycle = LifecycleRegistry(this)

    override val viewModelStore = ViewModelStore()

    private val savedStateController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory = SavedStateViewModelFactory()

    override val defaultViewModelCreationExtras: CreationExtras
        get() = MutableCreationExtras().apply {
            set(SAVED_STATE_REGISTRY_OWNER_KEY, this@DesktopScreenLifecycleOwner)
            set(VIEW_MODEL_STORE_OWNER_KEY, this@DesktopScreenLifecycleOwner)
        }

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        enableSavedStateHandles()
        lifecycle.currentState = Lifecycle.State.CREATED
    }

    @Composable
    override fun ProvideBeforeScreenContent(
        provideSaveableState: @Composable (suffixKey: String, content: @Composable () -> Unit) -> Unit,
        content: @Composable () -> Unit,
    ) {
        val enclosing = LocalLifecycleOwner.current
        DisposableEffect(enclosing) {
            val follow = LifecycleEventObserver { source, _ -> moveTo(source.lifecycle.currentState) }
            enclosing.lifecycle.addObserver(follow)
            onDispose {
                enclosing.lifecycle.removeObserver(follow)
                moveTo(Lifecycle.State.CREATED)
            }
        }
        CompositionLocalProvider(
            LocalLifecycleOwner provides this,
            LocalViewModelStoreOwner provides this,
            LocalSavedStateRegistryOwner provides this,
            content = content,
        )
    }

    override fun onDispose(screen: Screen) = destroy()

    /** Clears the ViewModels and ends the lifecycle, for an owner made with [detached] once its content is gone. */
    fun destroy() {
        viewModelStore.clear()
        lifecycle.currentState = Lifecycle.State.DESTROYED
    }

    // Only the navigator destroys a screen, and a destroyed lifecycle can't move again: the outgoing screen's content
    // leaves the composition after its navigator has disposed of it.
    private fun moveTo(state: Lifecycle.State) {
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) return
        lifecycle.currentState = state.coerceIn(Lifecycle.State.CREATED, Lifecycle.State.RESUMED)
    }

    /** Gives each screen of a navigator its owner; install it with Voyager's `LocalNavigatorScreenLifecycleProvider`. */
    companion object : NavigatorScreenLifecycleProvider {
        override fun provide(screen: Screen): List<ScreenLifecycleContentProvider> = listOf(get(screen))

        /** The owner of [screen], the same one its navigator renders it with. */
        fun get(screen: Screen): DesktopScreenLifecycleOwner =
            ScreenLifecycleStore.get(screen) { DesktopScreenLifecycleOwner() }

        /**
         * An owner of no screen, for content that outlives the screens it shows over, such as a player that plays on
         * in another window; it lasts until [destroy].
         */
        fun detached(): DesktopScreenLifecycleOwner = DesktopScreenLifecycleOwner()
    }
}
