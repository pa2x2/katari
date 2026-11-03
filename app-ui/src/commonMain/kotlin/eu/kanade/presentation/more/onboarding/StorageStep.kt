package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.presentation.more.settings.screen.data.collectPickedStorageDirectory
import eu.kanade.presentation.more.settings.screen.data.storageLocationDisplayPath
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.rememberScreenMessages
import mihon.presentation.kit.list.ControlRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

internal class StorageStep(
    private val storagePreferences: StoragePreferences,
    private val migratingFromMihon: Boolean,
) : OnboardingStep {

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    @Composable
    override fun Content() {
        val messages = rememberScreenMessages()
        val handler = LocalUriHandler.current

        val pickStorageLocation = SettingsDataScreen.storageLocationPicker(storagePreferences.baseStorageDirectory)
        val pickedLocation = collectPickedStorageDirectory(storagePreferences)
        val pickedLocationPath = pickedLocation?.let { storageLocationDisplayPath(it) }

        Column {
            RowGroup(
                rows = listOf {
                    ControlRow {
                        Text(
                            stringResource(
                                MR.strings.onboarding_storage_folder_info,
                                stringResource(MR.strings.app_name),
                            ),
                        )
                        if (pickedLocationPath != null) {
                            Text(stringResource(MR.strings.onboarding_storage_selected_folder, pickedLocationPath))
                        }
                        if (migratingFromMihon) {
                            Text(text = stringResource(MR.strings.onboarding_mihon_migration_storage))
                        }
                        Button(
                            label = stringResource(MR.strings.onboarding_storage_action_select),
                            onClick = {
                                if (!pickStorageLocation.launch()) {
                                    messages.show(MR.strings.file_picker_error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
            RowGroupGap()
            RowGroup(
                rows = listOf {
                    ControlRow {
                        Text(
                            stringResource(
                                MR.strings.onboarding_storage_help_info,
                                stringResource(MR.strings.app_name),
                            ),
                        )
                        Button(
                            label = stringResource(MR.strings.onboarding_storage_help_action),
                            onClick = { handler.openUri(SettingsDataScreen.HELP_URL) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
        }

        LaunchedEffect(pickedLocation) {
            _isComplete = pickedLocation != null
        }
    }
}
