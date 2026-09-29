package eu.kanade.presentation.more.settings.screen.textrecognition.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import mihon.model.artifacts.api.state.StoredModelArtifact
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.text.recognition.ui.language.shortDisplayNames
import mihon.text.recognition.ui.models.TextRecognitionStoredModels
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

/**
 * Lists every stored model revision, complete or partial, by what the profile reads with it, and lets the user
 * reclaim storage. Deleting a model the profile still reads with asks first.
 */
internal class ModelArtifactStorageScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { ModelArtifactStorageScreenModel() }
        val stored by model.stored.collectAsState()
        var confirming by remember { mutableStateOf<TextRecognitionStoredModels.InUse?>(null) }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.model_artifacts_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            val models = stored
            when {
                models == null -> LoadingScreen(Modifier.padding(contentPadding))
                models.totalBytes == 0L -> EmptyScreen(
                    stringRes = MR.strings.model_artifacts_empty,
                    modifier = Modifier.padding(contentPadding),
                )
                else -> LazyColumn(contentPadding = contentPadding) {
                    item(key = "summary") { StorageSummary(models, onFree = model::deleteObsolete) }
                    section(MR.strings.text_recognition_models_in_use, models.inUse.map { it.artifact }) { artifact ->
                        val use = models.inUse.first { it.artifact == artifact }
                        StoredModelRow(artifact, usage = use.usageLabel(), onDelete = { confirming = use })
                    }
                    section(MR.strings.text_recognition_models_unused, models.unused) { artifact ->
                        StoredModelRow(artifact, usage = null, onDelete = { model.delete(artifact.descriptor.id) })
                    }
                    section(MR.strings.text_recognition_models_obsolete, models.obsolete) { artifact ->
                        StoredModelRow(
                            artifact = artifact,
                            usage = stringResource(MR.strings.text_recognition_models_obsolete_reason),
                            onDelete = { model.delete(artifact.descriptor.id) },
                        )
                    }
                }
            }
        }
        confirming?.let { use ->
            AlertDialog(
                onDismissRequest = { confirming = null },
                title = {
                    Text(
                        stringResource(
                            MR.strings.text_recognition_models_delete_title,
                            use.artifact.descriptor.displayName,
                        ),
                    )
                },
                text = {
                    val size = formatModelArtifactSize(use.artifact.descriptor.sizeBytes)
                    Text(
                        if (use.everyLanguage) {
                            stringResource(MR.strings.text_recognition_models_delete_message_every_language, size)
                        } else {
                            stringResource(
                                MR.strings.text_recognition_models_delete_message,
                                use.languages.shortDisplayNames(),
                                size,
                            )
                        },
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirming = null
                            model.delete(use.artifact.descriptor.id)
                        },
                    ) {
                        Text(stringResource(MR.strings.action_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirming = null }) {
                        Text(stringResource(MR.strings.action_cancel))
                    }
                },
            )
        }
    }
}

private fun LazyListScope.section(
    title: StringResource,
    artifacts: List<StoredModelArtifact>,
    row: @Composable (StoredModelArtifact) -> Unit,
) {
    if (artifacts.isEmpty()) return
    item(key = "heading-${title.resourceId}") { HeadingItem(title) }
    items(artifacts, key = { "${it.descriptor.id.value}@${it.descriptor.revision}" }) { row(it) }
}

@Composable
private fun StorageSummary(models: TextRecognitionStoredModels, onFree: () -> Unit) {
    val count = models.inUse.size + models.unused.size + models.obsolete.size
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.padding.large),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatModelArtifactSize(models.totalBytes), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = pluralStringResource(MR.plurals.model_artifacts_count, count, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (models.obsolete.isNotEmpty()) {
                FilledTonalButton(onClick = onFree) {
                    Text(stringResource(MR.strings.model_artifacts_free, formatModelArtifactSize(models.obsoleteBytes)))
                }
            }
        }
    }
}

@Composable
private fun StoredModelRow(artifact: StoredModelArtifact, usage: String?, onDelete: () -> Unit) {
    val size = formatModelArtifactSize(artifact.storedBytes)
    val state = if (artifact.complete) {
        stringResource(MR.strings.model_artifacts_state_installed, size)
    } else {
        stringResource(MR.strings.model_artifacts_state_partial, size)
    }
    ListItem(
        supportingContent = { Text(listOfNotNull(state, usage).joinToString(" · ")) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(MR.strings.action_delete))
            }
        },
    ) {
        Text(artifact.descriptor.displayName)
    }
}

@Composable
private fun TextRecognitionStoredModels.InUse.usageLabel(): String =
    if (everyLanguage) {
        stringResource(MR.strings.text_recognition_models_used_for_every_language)
    } else {
        stringResource(MR.strings.text_recognition_models_used_for, languages.shortDisplayNames())
    }
