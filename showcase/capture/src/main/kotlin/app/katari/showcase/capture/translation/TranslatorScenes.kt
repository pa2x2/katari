package app.katari.showcase.capture.translation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.openProfile
import app.katari.showcase.capture.openTab
import org.junit.Test
import org.junit.runner.RunWith

/** The Translator tab, which the seed puts in the Personal profile's bar, translating a line typed into it. */
@RunWith(AndroidJUnit4::class)
class TranslatorScenes {
    @Test
    fun translator() {
        openProfile("Personal")
        // A tap while the library is still loading after the profile picker is lost.
        settle()
        openTab("Translator")
        find(By.clazz("android.widget.EditText")).text = RASHOMON_OPENING
        // Its read-aloud button appears with the translation.
        find(By.desc("Listen to translation"))
        capture("translator")
    }

    private companion object {
        // The opening of Rashōmon, which the Personal profile has read in the original.
        const val RASHOMON_OPENING = "ある日の暮方の事である。一人の下人が、羅生門の下で雨やみを待っていた。"
    }
}
