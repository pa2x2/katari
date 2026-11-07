package app.katari.sync.server.web

import app.katari.sync.server.SyncServices
import app.katari.sync.server.http.group
import app.katari.sync.server.web.page.ASSETS_PATH
import app.katari.sync.server.web.request.webRequests
import app.katari.sync.server.web.route.adminRoutes
import app.katari.sync.server.web.route.ownAccountRoutes
import app.katari.sync.server.web.route.setupRoutes
import app.katari.sync.server.web.route.signInRoutes
import app.katari.sync.server.web.route.startRoute
import io.ktor.http.CacheControl
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing

/**
 * The server's own web page, beside the sync API: setting the server up, signing in, admins managing accounts, and
 * everyone managing their own devices and data. Plain HTML forms that work without JavaScript, rendered here; the one
 * script only adds copy buttons.
 */
fun Application.webPages(services: SyncServices) {
    routing {
        staticResources(ASSETS_PATH, "web") {
            cacheControl { listOf(CacheControl.MaxAge(maxAgeSeconds = ASSET_MAX_AGE_SECONDS)) }
        }
        group("web pages") {
            install(webRequests(services))
            setupRoutes(services)
            signInRoutes(services)
            startRoute(services)
            adminRoutes(services)
            ownAccountRoutes(services)
        }
    }
}

// An hour: assets change only with the server's version, and a new one is picked up soon after an upgrade.
private const val ASSET_MAX_AGE_SECONDS = 60 * 60
