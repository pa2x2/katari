package app.katari.sync.ui.question

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The top of a question about an entry: what happened to it, the profile it is in, named on every question since the
 * entry may be in one other than the profile in use (D017), and what every device shows until it is answered.
 */
@Composable
internal fun EntryQuestionHeader(title: String, profile: QuestionProfile?, text: String, icon: ImageVector) {
    SettingsRow(
        title = title,
        subtitle = profile?.let { stringResource(MR.strings.sync_question_in_profile, it.name) },
        subtitleTone = ContentTone.Primary,
        icon = icon,
        below = { Text(text, role = TextRole.Supporting) },
    )
}
