package eu.kanade.tachiyomi.ui.base.observation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

/** Seeds [item] into the store and follows its observed updates while composed. */
@Composable
fun <T : Any> ObservedItemStore<*, T>.collectAsState(item: T): State<T> {
    val observed = remember(this, item) { seed(item) }.collectAsState()
    return remember(observed, item) { derivedStateOf { observed.value ?: item } }
}
