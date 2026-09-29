package eu.kanade.tachiyomi.ui.browse.source.browse.preset

import eu.kanade.domain.source.model.SourceFeedPreset

/**
 * Preset state and actions offered by the filter sheet header.
 *
 * [currentPresetId] and [currentPresetName] describe the preset the draft stands for, if any: the custom preset it
 * was loaded from, or the built-in listing it equals. [onUpdateCurrent] is offered only for a custom one.
 */
class SourceFilterPresetActions(
    val presets: List<SourceFeedPreset>,
    val currentPresetId: String?,
    val currentPresetName: String?,
    val onApply: (String) -> Unit = {},
    val onEdit: (String) -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val canDelete: (String) -> Boolean = { false },
    val onSaveAsNew: (() -> Unit)? = null,
    val onUpdateCurrent: (() -> Unit)? = null,
) {
    internal val hasMenu: Boolean
        get() = presets.isNotEmpty() || onSaveAsNew != null ||
            (onUpdateCurrent != null && currentPresetName != null)
}
