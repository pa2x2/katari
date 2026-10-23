package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.extension.model.SourceConflict
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** One line per source an extension doesn't provide, naming what does. */
@Composable
internal fun sourceConflictLines(conflicts: List<SourceConflict>): List<String> {
    val context = LocalContext.current
    return conflicts.map { conflict ->
        val source = if (conflict.sourceLang.isBlank()) {
            conflict.sourceName
        } else {
            "${conflict.sourceName} (${LocaleHelper.getSourceDisplayName(conflict.sourceLang, context)})"
        }
        when (val providedBy = conflict.providedBy) {
            null -> stringResource(MR.strings.ext_source_conflict_id_taken, source)
            else -> stringResource(MR.strings.ext_source_conflict_provided_by, source, providedBy)
        }
    }
}
