package eu.kanade.presentation.more.settings.screen.textrules

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.app.di.appGraph
import mihon.entry.interactions.reader.settings.BookTextRule
import mihon.entry.interactions.reader.settings.BookTextRuleScope
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.icons.materialsymbols.rounded.Delete
import mihon.presentation.kit.button.FloatingActionButton
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.shouldExpandFAB
import androidx.compose.runtime.collectAsState as collectFlowAsState

/** The profile's text rules for the book reader: switched on and off, edited, and added for a book source. */
class BookTextRulesScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val preference = remember { context.appGraph.bookDocumentReaderSettings.textRules }
        val rules by preference.collectAsState()
        val sourceRepository = remember { context.appGraph.sourceRepository }
        val allSources by sourceRepository.getSources().collectFlowAsState(initial = emptyList())
        val bookSources = remember(allSources) {
            allSources
                .filter { it.supportedEntryTypes?.contains(EntryType.BOOK) == true }
                .distinctBy { it.id }
                .sortedBy { it.visualName.lowercase() }
                .map { BookTextRuleScope.Source(it.id, it.visualName) }
        }
        val lazyListState = rememberLazyListState()
        var dialog by remember { mutableStateOf<BookTextRuleDialog?>(null) }

        fun replace(rule: BookTextRule) = preference.set(rules.map { if (it.id == rule.id) rule else it })

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_book_text_rules),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    label = stringResource(MR.strings.action_add),
                    icon = MaterialSymbols.Rounded.Add,
                    onClick = { dialog = BookTextRuleDialog.Create },
                    expanded = lazyListState.shouldExpandFAB(),
                )
            },
        ) { paddingValues ->
            LazyColumn(
                state = lazyListState,
                contentPadding = paddingValues + PaddingValues(top = KatariTheme.spacing.s),
            ) {
                item { RowGroupNote(stringResource(MR.strings.book_text_rules_help)) }
                if (rules.isEmpty()) {
                    item { EmptyScreen(MR.strings.book_text_rules_empty) }
                } else {
                    itemsIndexed(rules, key = { _, rule -> rule.id }) { index, rule ->
                        RowGroupItem(RowGroupPosition.of(index, rules.size)) {
                            BookTextRuleItem(
                                rule = rule,
                                onEnabledChange = { replace(rule.copy(enabled = it)) },
                                onEdit = { dialog = BookTextRuleDialog.Edit(rule) },
                                onDelete = { dialog = BookTextRuleDialog.Delete(rule) },
                            )
                        }
                    }
                }
            }
        }

        when (val current = dialog) {
            null -> Unit
            BookTextRuleDialog.Create -> BookTextRuleEditorDialog(
                editing = null,
                sources = bookSources,
                onSave = { rule ->
                    preference.set(rules + rule)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
            is BookTextRuleDialog.Edit -> BookTextRuleEditorDialog(
                editing = current.rule,
                sources = bookSources,
                onSave = { rule ->
                    replace(rule)
                    dialog = null
                },
                onDismiss = { dialog = null },
                onDelete = { dialog = BookTextRuleDialog.Delete(current.rule) },
            )
            is BookTextRuleDialog.Delete -> ConfirmDialog(
                title = stringResource(MR.strings.book_text_rule_delete),
                text = stringResource(MR.strings.book_text_rule_delete_confirmation, current.rule.find),
                confirmLabel = stringResource(MR.strings.action_delete),
                onConfirm = {
                    preference.set(rules.filterNot { it.id == current.rule.id })
                    dialog = null
                },
                onDismissRequest = { dialog = null },
                destructive = true,
                icon = MaterialSymbols.Rounded.Delete,
            )
        }
    }
}

private sealed interface BookTextRuleDialog {
    data object Create : BookTextRuleDialog

    data class Edit(val rule: BookTextRule) : BookTextRuleDialog

    data class Delete(val rule: BookTextRule) : BookTextRuleDialog
}
