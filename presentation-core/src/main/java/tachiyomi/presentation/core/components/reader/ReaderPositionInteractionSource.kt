package tachiyomi.presentation.core.components.reader

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource

/** Material calls drag-stopped for cancellation too; deferred seeks must distinguish that from release. */
internal class ReaderPositionInteractionSource(
    private val onCancel: () -> Unit,
    private val delegate: MutableInteractionSource = MutableInteractionSource(),
) : MutableInteractionSource by delegate {
    var cancelled = false
        private set

    override suspend fun emit(interaction: Interaction) {
        observe(interaction)
        delegate.emit(interaction)
    }

    override fun tryEmit(interaction: Interaction): Boolean {
        observe(interaction)
        return delegate.tryEmit(interaction)
    }

    private fun observe(interaction: Interaction) {
        when (interaction) {
            is DragInteraction.Start -> cancelled = false
            is DragInteraction.Cancel -> {
                cancelled = true
                onCancel()
            }
        }
    }

    fun consumeCancellation(): Boolean = cancelled.also { cancelled = false }
}
