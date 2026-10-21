package eu.kanade.presentation.more.onboarding

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.ControlRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

internal class StorageStep(
    storagePreferences: StoragePreferences,
    private val migratingFromMihon: Boolean,
) : OnboardingStep {

    private val storagePref = storagePreferences.baseStorageDirectory

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val handler = LocalUriHandler.current

        val pickStorageLocation = SettingsDataScreen.storageLocationPicker(storagePref)

        Column {
            RowGroup(
                rows = listOf {
                    ControlRow {
                        Text(
                            stringResource(
                                MR.strings.onboarding_storage_info,
                                stringResource(MR.strings.app_name),
                                SettingsDataScreen.storageLocationText(storagePref),
                            ),
                        )
                        if (migratingFromMihon) {
                            Text(text = stringResource(MR.strings.onboarding_mihon_migration_storage))
                        }
                        Button(
                            label = stringResource(MR.strings.onboarding_storage_action_select),
                            onClick = {
                                try {
                                    pickStorageLocation.launch(null)
                                } catch (e: ActivityNotFoundException) {
                                    context.toast(MR.strings.file_picker_error)
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

        LaunchedEffect(Unit) {
            storagePref.changes()
                .collectLatest { _isComplete = storagePref.isSet() }
        }
    }
}
