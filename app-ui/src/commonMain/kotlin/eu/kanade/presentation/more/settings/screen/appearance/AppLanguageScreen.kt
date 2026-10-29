package eu.kanade.presentation.more.settings.screen.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.platform.PlatformContext
import mihon.core.platform.currentPlatformContext
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.i18n.platform.stringResource as platformStringResource

/**
 * The app's own language, from the languages it has strings in, or the system's. Where a pick only applies when the
 * app starts again, the app offers to restart.
 */
class AppLanguageScreen : Screen() {

    @Composable
    override fun Content() {
        val context = currentPlatformContext()
        val navigator = LocalNavigator.currentOrThrow
        val store = LocalAppUiGraph.current.appLocaleStore

        val langs = remember { getLangs(context) }
        var currentLanguage by remember { mutableStateOf(store.picked?.toLanguageTag() ?: "") }
        var askToRestart by rememberSaveable { mutableStateOf(false) }

        if (askToRestart) {
            ConfirmDialog(
                title = stringResource(MR.strings.restart_app_title),
                text = stringResource(MR.strings.restart_app_language),
                confirmLabel = stringResource(MR.strings.action_restart),
                onConfirm = {
                    askToRestart = false
                    store.restartToApply()
                },
                onDismissRequest = { askToRestart = false },
                dismissLabel = stringResource(MR.strings.action_later),
            )
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_app_language),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            // The system default stands apart from the languages it can resolve to.
            val (systemDefault, languages) = langs.partition { it.langTag.isEmpty() }
            val onSelect: (String) -> Unit = { langTag ->
                if (langTag != currentLanguage) {
                    currentLanguage = langTag
                    store.set(langTag.ifEmpty { null })
                    if (!store.appliesAtOnce) askToRestart = true
                }
            }
            RowList(contentPadding = contentPadding) {
                rowGroupItems(systemDefault, key = Language::langTag) { LanguageRow(it, currentLanguage, onSelect) }
                item { RowGroupGap() }
                rowGroupItems(languages, key = Language::langTag) { LanguageRow(it, currentLanguage, onSelect) }
            }
        }
    }

    @Composable
    private fun LanguageRow(language: Language, currentLanguage: String, onSelect: (String) -> Unit) {
        ChoiceRow(
            title = language.displayName,
            selected = currentLanguage == language.langTag,
            onClick = { onSelect(language.langTag) },
            subtitle = language.localizedDisplayName,
        )
    }

    private fun getLangs(context: PlatformContext): List<Language> = buildList {
        AppLocaleTags.forEach { langTag ->
            val displayName = LocaleHelper.getLocalizedDisplayName(langTag)
            if (displayName.isNotEmpty()) {
                add(Language(langTag, displayName, LocaleHelper.getDisplayName(langTag)))
            }
        }

        sortBy { it.displayName }
        add(0, Language("", context.platformStringResource(MR.strings.label_default), null))
    }

    private data class Language(
        val langTag: String,
        val displayName: String,
        val localizedDisplayName: String?,
    )
}
