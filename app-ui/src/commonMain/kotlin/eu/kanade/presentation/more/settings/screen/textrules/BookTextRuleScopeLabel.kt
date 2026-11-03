package eu.kanade.presentation.more.settings.screen.textrules

import androidx.compose.runtime.Composable
import mihon.entry.interactions.reader.settings.BookTextRuleScope
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** What a rule applies to, as its group in the list and its editor name it. */
@Composable
internal fun BookTextRuleScope.label(): String = when (this) {
    is BookTextRuleScope.Series -> title
    is BookTextRuleScope.Source -> stringResource(MR.strings.book_text_rule_scope_source, name)
}

/**
 * Which group a rule is listed in. A series is told apart by its source and URL alone, so rules saved under an older
 * title of the same series stay together.
 */
internal val BookTextRuleScope.groupKey: String
    get() = when (this) {
        is BookTextRuleScope.Series -> "series:$sourceId:$entryUrl"
        is BookTextRuleScope.Source -> "source:$sourceId"
    }
