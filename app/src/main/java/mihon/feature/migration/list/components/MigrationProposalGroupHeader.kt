package mihon.feature.migration.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.feature.migration.list.models.MigrationMergeContext
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The merged entry whose members follow in the migration list, with how many members it has. */
@Composable
fun MigrationProposalGroupHeader(context: MigrationMergeContext) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        Text(
            text = context.rootTitle,
            role = TextRole.SectionTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(MR.strings.merge_members_count, context.memberCount),
            role = TextRole.Supporting,
        )
    }
}
