package app.katari.entry.interactions.runtime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.katari.core.metro.metroGraph

/** Base activity for reader and player processors that use the host application's theme. */
abstract class EntryInteractionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        metroGraph<EntryInteractionActivityGraph>().entryInteractionActivityTheme.apply(this)
        super.onCreate(savedInstanceState)
    }
}
