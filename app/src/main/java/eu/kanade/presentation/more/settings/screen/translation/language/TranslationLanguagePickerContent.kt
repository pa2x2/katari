package eu.kanade.presentation.more.settings.screen.translation.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import eu.kanade.presentation.components.AppBar
import mihon.translation.api.TranslationLanguageTag
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale

@Composable
internal fun TranslationLanguagePickerContent(
    title: String,
    options: List<TranslationLanguageOption>,
    selected: TranslationLanguageTag?,
    includeAppLanguage: Boolean,
    onSelect: (TranslationLanguageTag?) -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(options, query) {
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            options
        } else {
            options.filter { option ->
                option.displayName.contains(normalized, ignoreCase = true) ||
                    option.tag.value.contains(normalized, ignoreCase = true)
            }
        }
    }
    Scaffold(
        topBar = {
            AppBar(
                title = title,
                navigateUp = onBack,
                scrollBehavior = it,
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.padding.medium),
                label = { Text(stringResource(MR.strings.translation_settings_language_search)) },
                singleLine = true,
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = MaterialTheme.padding.small),
            ) {
                if (includeAppLanguage) {
                    item(key = "app-language") {
                        LanguageRow(
                            label = defaultTranslationTargetLabel(),
                            supporting = stringResource(
                                MR.strings.translation_settings_follows_app_language,
                            ),
                            selected = selected == null,
                            onClick = { onSelect(null) },
                        )
                    }
                }
                items(
                    items = filtered,
                    key = { it.tag.value },
                ) { option ->
                    LanguageRow(
                        label = option.displayName,
                        supporting = option.tag.value,
                        selected = option.tag == selected,
                        onClick = { onSelect(option.tag) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(
    label: String,
    supporting: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(supporting) },
        trailingContent = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
internal fun defaultTranslationTargetLabel(): String {
    val locale = AppCompatDelegate.getApplicationLocales().get(0)
        ?: LocaleListCompat.getAdjustedDefault().get(0)
        ?: Locale.getDefault()
    val name = locale.getDisplayName(Locale.getDefault()).ifBlank { locale.toLanguageTag() }
    return stringResource(MR.strings.translation_settings_target_default, name)
}
