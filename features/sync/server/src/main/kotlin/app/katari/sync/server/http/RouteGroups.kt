package app.katari.sync.server.http

import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext

/** Routes under it share its plugins without adding to their paths. */
internal fun Route.group(name: String, build: Route.() -> Unit): Route = createChild(GroupSelector(name)).apply(build)

private class GroupSelector(private val name: String) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Transparent

    override fun toString(): String = "($name)"
}
