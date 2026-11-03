package mihon.entry.interactions.runtime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import mihon.core.metro.metroGraph

/** Base activity for reader and player processors that use the host application's theme. */
abstract class EntryInteractionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        metroGraph<EntryInteractionActivityGraph>().entryInteractionActivityTheme.apply(this)
        super.onCreate(savedInstanceState)
    }
}
