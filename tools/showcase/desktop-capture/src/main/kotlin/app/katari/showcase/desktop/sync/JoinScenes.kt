package app.katari.showcase.desktop.sync

import app.katari.showcase.desktop.DesktopScene
import app.katari.showcase.desktop.SceneContext
import app.katari.showcase.desktop.chooseStorageFolder
import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.Selector
import app.katari.showcase.desktop.nextGuideStep
import app.katari.showcase.desktop.trustShowcaseExtension

/**
 * Sets the app up as the sync group's desktop scenes expect it: through its first-run guide as a newcomer, keeping
 * downloads in the `storage` folder and signing in to the phone's account (the `syncServer`, `syncUsername` and
 * `syncPassword` arguments), which brings the phone's library in. Then it trusts the showcase extension.
 */
internal class JoinScenes : DesktopScene {
    override fun run(scene: SceneContext) {
        // The guide starts with "I'm new to Katari" picked.
        nextGuideStep(awaiting = "Appearance")
        nextGuideStep(awaiting = "Storage")
        chooseStorageFolder(scene.argument("storage"))
        nextGuideStep(awaiting = "Use Katari on more devices?")
        Desktop.click(Selector.text("Sign in with server address"))
        Desktop.enterText(Selector.field("Server address"), scene.argument("syncServer"))
        Desktop.enterText(Selector.field("Username"), scene.argument("syncUsername"))
        Desktop.enterText(Selector.field("Password"), scene.argument("syncPassword"))
        Desktop.click(Selector.button("Sign in"))
        Desktop.find(Selector.text("This device is linked"), SIGN_IN_TIMEOUT_MS)
        nextGuideStep(awaiting = "You're all set")
        Desktop.click(Selector.text("Get started"))
        trustShowcaseExtension()
    }

    private companion object {
        // Signing in takes in the account's library; a CI runner is slow at it.
        const val SIGN_IN_TIMEOUT_MS = 3 * 60_000L
    }
}
