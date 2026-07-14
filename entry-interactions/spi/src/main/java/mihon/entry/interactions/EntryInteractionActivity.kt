package mihon.entry.interactions

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Base activity for reader and player processors that use the host application's theme. */
abstract class EntryInteractionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Injekt.get<EntryInteractionActivityTheme>().apply(this)
        super.onCreate(savedInstanceState)
    }
}
