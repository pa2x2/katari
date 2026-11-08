package app.katari.sourcemigration.review.components

import androidx.compose.runtime.Composable
import app.katari.sourcemigration.session.model.SourceMigrationMatchKind
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun sourceMigrationMatchKindLabel(matchKind: SourceMigrationMatchKind): String {
    return stringResource(
        when (matchKind) {
            SourceMigrationMatchKind.EXACT -> MR.strings.sourceMigrationReview_exactMatch
            SourceMigrationMatchKind.SIMILAR -> MR.strings.sourceMigrationReview_similarMatch
            SourceMigrationMatchKind.MANUAL -> MR.strings.sourceMigrationReview_manualMatch
        },
    )
}
