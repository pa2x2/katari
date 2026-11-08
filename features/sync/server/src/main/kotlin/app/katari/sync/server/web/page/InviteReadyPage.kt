package app.katari.sync.server.web.page

import app.katari.sync.protocol.link.InviteLink
import io.ktor.server.application.ApplicationCall
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.id
import kotlinx.html.p

/**
 * The invite's code, shown only in the answer that made it: the server keeps nothing it could show again. The server's
 * [address] goes with it, as the new person's app needs both, and so does a QR code of the two that the app's scanner
 * opens signing up with.
 */
internal suspend fun ApplicationCall.respondInviteReady(code: String, address: String) {
    respondPage("Invite ready") {
        centreCard {
            h1 { +"Invite ready" }
            p("lede path-intro") { +"Give this to the person. In Katari they open" }
            p("path") { +"Sync → Sign in with server address → I have an invite code" }
            p("lede") {
                +"and choose their username and password. On a phone they can scan the code instead, under "
                +"Sync → Scan a code from a linked device. It works once, for 1 day."
            }
            div("invite") {
                qrCode(InviteLink(address, code).encode(), label = "QR code of the server address and invite code")
                div("invite-details") {
                    id = "invite-details"
                    p("lbl") { +"Server address" }
                    p("secret address") { +address }
                    p("lbl") { +"Invite code" }
                    p("secret") { +code }
                }
            }
            actions {
                copyButton("Copy both", copies = "invite-details", classes = "bt t lead")
                linkButton("Done", "/", ButtonStyle.Filled)
            }
        }
    }
}
