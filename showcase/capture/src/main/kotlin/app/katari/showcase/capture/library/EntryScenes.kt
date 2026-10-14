package app.katari.showcase.capture.library

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openEntry
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryScenes {
    @Test
    fun activity() {
        openProfile("Personal")
        openEntry("The Adventures of Sherlock Holmes")
        tap(By.textContains("since"))
        find(By.text("Your activity"))
        capture("entry-activity")
    }

    @Test
    fun manageMerge() {
        openProfile("Personal")
        tap(By.text("Pepper&Carrot"))
        find(By.textContains("ペッパー"))
        tap(By.desc("More options"))
        tap(By.text("Manage merge"))
        find(By.text("Unmerge"))
        capture("manage-merge")
    }
}
