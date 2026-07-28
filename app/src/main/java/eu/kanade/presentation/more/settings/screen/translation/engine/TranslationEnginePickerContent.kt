package eu.kanade.presentation.more.settings.screen.translation.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.presentation.components.AppBar
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineId
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslationEnginePickerContent(
    engines: List<KnownTranslationEngine>,
    selected: TranslationEngineId,
    onSelect: (TranslationEngineId) -> Unit,
    onOpenDocumentation: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(MR.strings.translation_settings_engine),
                navigateUp = onBack,
                scrollBehavior = it,
            )
        },
    ) { contentPadding ->
        ScrollbarLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            if (engines.none { it.id == selected }) {
                item {
                    OutlinedCard(
                        modifier = Modifier
                            .padding(horizontal = MaterialTheme.padding.medium)
                            .fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(MaterialTheme.padding.large)) {
                            Text(
                                text = stringResource(
                                    MR.strings.translation_settings_engine_unknown,
                                    selected.value,
                                ),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(
                                    MR.strings.translation_settings_missing_engine_no_fallback,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            items(
                items = engines,
                key = { it.id.value },
            ) { engine ->
                val availability = engine.buildAvailability
                ElevatedCard(
                    onClick = { onSelect(engine.id) },
                    enabled = availability is TranslationEngineBuildAvailability.Included,
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.padding.medium)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(MaterialTheme.padding.large),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = engine.engineName,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = engine.providerName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (selected == engine.id) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        if (availability is TranslationEngineBuildAvailability.NotIncluded) {
                            Text(
                                text = availability.reason,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        engine.documentationUrl?.let { url ->
                            TextButton(onClick = { onOpenDocumentation(url) }) {
                                Text(stringResource(MR.strings.translation_settings_provider_details))
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(MR.strings.translation_settings_explicit_engine_notice),
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.large),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
internal fun translationEngineLabel(
    engine: TranslationEngineId,
    engines: List<KnownTranslationEngine>,
): String {
    val known = engines.firstOrNull { it.id == engine }
    return known?.let { "${it.engineName} · ${it.providerName}" }
        ?: stringResource(MR.strings.translation_settings_engine_unknown, engine.value)
}
