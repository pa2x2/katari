package eu.kanade.tachiyomi.ui.entry.missingsource

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.presentation.entry.components.MissingSourceSheet

/** Shows [model]'s sheet for the source [sourceName]; [onMigrate] moves the entry to another source. */
@Composable
fun EntryMissingSourceSheet(
    model: MissingSourceViewModel,
    sourceName: String,
    onMigrate: (() -> Unit)?,
) {
    val shown by model.sheetShown.collectAsStateWithLifecycle()
    if (!shown) return
    // Collected only while shown, so the store listings are looked up only for an open sheet
    val listing by model.listing.collectAsStateWithLifecycle()
    val installStep by model.installStep.collectAsStateWithLifecycle()
    MissingSourceSheet(
        sourceName = sourceName,
        listing = listing,
        installStep = installStep,
        onInstall = model::install,
        onMigrate = onMigrate,
        onDismissRequest = model::dismissSheet,
    )
}
