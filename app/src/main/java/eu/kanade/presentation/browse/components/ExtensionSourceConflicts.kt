package eu.kanade.presentation.browse.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.extension.model.SourceConflict
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.list.SettingsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** One line per source an extension doesn't provide, naming what does. */
@Composable
internal fun sourceConflictLines(conflicts: List<SourceConflict>): List<String> {
    val context = LocalContext.current
    return conflicts.map { conflict ->
        val source = conflict.sourceLabel(context)
        when (val providedBy = conflict.providedBy) {
            null -> stringResource(MR.strings.ext_source_conflict_id_taken, source)
            else -> stringResource(MR.strings.ext_source_conflict_provided_by, source, providedBy)
        }
    }
}

/** A source an extension has but another provides, with the action that makes this extension provide it instead. */
@Composable
internal fun ExtensionSourceConflictRow(
    conflict: SourceConflict,
    onClickUseForSource: (sourceKey: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsRow(
        title = conflict.sourceLabel(LocalContext.current),
        modifier = modifier,
        subtitle = when (val providedBy = conflict.providedBy) {
            null -> stringResource(MR.strings.ext_source_id_taken)
            else -> stringResource(MR.strings.ext_source_provided_by, providedBy)
        },
        trailing = conflict.sourceKey?.let { sourceKey ->
            {
                Button(
                    label = stringResource(MR.strings.ext_use_this_extension),
                    onClick = { onClickUseForSource(sourceKey) },
                    style = ButtonStyle.Tonal,
                )
            }
        },
    )
}

private fun SourceConflict.sourceLabel(context: Context): String {
    return if (sourceLang.isBlank()) {
        sourceName
    } else {
        "$sourceName (${LocaleHelper.getSourceDisplayName(sourceLang, context)})"
    }
}
