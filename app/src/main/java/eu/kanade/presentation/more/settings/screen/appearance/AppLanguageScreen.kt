package eu.kanade.presentation.more.settings.screen.appearance

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import org.xmlpull.v1.XmlPullParser
import tachiyomi.i18n.*
import tachiyomi.i18n.R
import tachiyomi.i18n.context.stringResource
import tachiyomi.presentation.core.i18n.stringResource

class AppLanguageScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val langs = remember { getLangs(context) }
        var currentLanguage by remember {
            mutableStateOf(AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag() ?: "")
        }

        LaunchedEffect(currentLanguage) {
            val locale = if (currentLanguage.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(currentLanguage)
            }
            AppCompatDelegate.setApplicationLocales(locale)
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
            val onSelect: (String) -> Unit = { currentLanguage = it }
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

    private fun getLangs(context: Context): List<Language> = buildList {
        val parser = context.resources.getXml(R.xml.locales_config)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                for (i in 0..<parser.attributeCount) {
                    if (parser.getAttributeName(i) == "name") {
                        val langTag = parser.getAttributeValue(i)
                        val displayName = LocaleHelper.getLocalizedDisplayName(langTag)
                        if (displayName.isNotEmpty()) {
                            add(Language(langTag, displayName, LocaleHelper.getDisplayName(langTag)))
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        sortBy { it.displayName }
        add(0, Language("", context.stringResource(MR.strings.label_default), null))
    }

    private data class Language(
        val langTag: String,
        val displayName: String,
        val localizedDisplayName: String?,
    )
}
