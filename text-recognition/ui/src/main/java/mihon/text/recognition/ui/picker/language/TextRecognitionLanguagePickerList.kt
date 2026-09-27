package mihon.text.recognition.ui.picker.language

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.ui.language.displayName

/** Languages this build can read, alphabetically by their name in the app language. */
@Composable
fun TextRecognitionLanguagePickerList(
    languages: List<LanguageTag>,
    onSelect: (LanguageTag) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val named = languages.map { it to it.displayName() }.sortedBy { it.second }
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        items(named, key = { it.first.value }) { (language, name) ->
            ListItem(
                modifier = Modifier.clickable { onSelect(language) },
                content = { Text(name) },
            )
        }
    }
}
