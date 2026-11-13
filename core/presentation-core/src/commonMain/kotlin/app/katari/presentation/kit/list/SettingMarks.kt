package app.katari.presentation.kit.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.selection.secondaryClick
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.core.common.preference.Preference

/**
 * What the app shows after the title of a row that edits a setting, such as whether the setting syncs. Rows given the
 * setting they edit ([SettingsRow], [ToggleRow], [SliderRow], [StepperRow], [ChipsRow]) draw it on their title's line,
 * in Settings and in the readers' and players' sheets alike; where the app provides none, they show their title alone.
 * A mark opens what it stands for, such as the setting's sync choice; a right-click anywhere on the row opens it too.
 */
interface SettingMarks {
    /** Whether [setting] has a mark at all, so a row without one leaves its right-click alone. */
    fun hasMark(setting: Preference<*>): Boolean

    /**
     * The mark of [setting], whose row is titled [title] in [role]'s text, and what it opens while [open]. The row
     * keeps [open], so a right-click on it opens the same thing.
     */
    @Composable
    fun Mark(setting: Preference<*>, title: String, role: TextRole, open: Boolean, onOpenChange: (Boolean) -> Unit)
}

/** The app's [SettingMarks]; null where it gives none. */
val LocalSettingMarks = staticCompositionLocalOf<SettingMarks?> { null }

/** A row's mark: what follows its title, and the right-click on the row that opens what the mark opens. */
internal class RowSettingMark(val titleSuffix: (@Composable () -> Unit)?, val rowModifier: Modifier)

/** [suffix] followed by [setting]'s mark, for a title in [role]'s text, with the right-click that opens it. */
@Composable
internal fun rememberRowSettingMark(
    setting: Preference<*>?,
    title: String,
    role: TextRole,
    suffix: (@Composable () -> Unit)?,
): RowSettingMark {
    val marks = LocalSettingMarks.current
    var open by rememberSaveable { mutableStateOf(false) }
    if (setting == null || marks == null || !marks.hasMark(setting)) return RowSettingMark(suffix, Modifier)
    return RowSettingMark(
        titleSuffix = {
            suffix?.invoke()
            marks.Mark(setting, title, role, open) { open = it }
        },
        rowModifier = Modifier.secondaryClick(enabled = true) { open = true },
    )
}
