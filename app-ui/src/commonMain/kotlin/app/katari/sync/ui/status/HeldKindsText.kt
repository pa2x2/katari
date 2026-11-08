package app.katari.sync.ui.status

import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What the records of [kinds] are to the person, such as "categories, fonts". A kind is named by what it is part of,
 * from the first part of its name, since a held kind is often one this app version doesn't know at all; one it can't
 * place is newer data.
 */
@Composable
internal fun heldKindsText(kinds: List<String>): String =
    kinds.map { kindLabel(it.substringBefore('.')) }.distinct().map { stringResource(it) }.joinToString()

private fun kindLabel(family: String): StringResource = when (family) {
    "entry", "entries" -> MR.strings.sync_kind_entries
    "chapter", "chapters" -> MR.strings.sync_kind_chapters
    "progress" -> MR.strings.sync_kind_progress
    "history" -> MR.strings.sync_kind_history
    "activity", "statistics" -> MR.strings.sync_kind_statistics
    "category", "categories" -> MR.strings.sync_kind_categories
    "setting" -> MR.strings.sync_kind_settings
    "font" -> MR.strings.sync_kind_fonts
    "profile" -> MR.strings.sync_kind_profiles
    "extension" -> MR.strings.sync_kind_extension_repositories
    "merge" -> MR.strings.sync_kind_merges
    "question" -> MR.strings.sync_kind_questions
    else -> MR.strings.sync_kind_newer
}
