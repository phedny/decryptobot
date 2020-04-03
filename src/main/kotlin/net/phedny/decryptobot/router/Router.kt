package net.phedny.decryptobot.router

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.phedny.decryptobot.command.Command
import net.phedny.decryptobot.command.PrivateMessageCommand

class Router private constructor(private val prefix:String, private val routes:List<Route>, private val privateMessageRoutes:List<PrivateMessageRoute>) {

    fun process(event: GuildMessageReceivedEvent) {
        val matchingRoute = routes.firstOrNull { event.message.contentRaw.startsWith("$prefix${it.path}", true) }?: return
        matchingRoute.command.execute(event, "$prefix${matchingRoute.path}")
    }

    fun process(event: PrivateMessageReceivedEvent) {
        val matchingRoute = privateMessageRoutes.firstOrNull { event.message.contentRaw.startsWith("$prefix${it.path}", true) }?: return
        matchingRoute.command.execute(event, "$prefix${matchingRoute.path}")
    }

    class Builder(private val prefix: String = "?") {
        private val routes = mutableListOf<Route>()
        private val pmRoutes = mutableListOf<PrivateMessageRoute>()
        fun add(path: String, command: Command) = apply { routes.add(Route(path, command))}
        fun add(path: String, command: PrivateMessageCommand) = apply { pmRoutes.add(PrivateMessageRoute(path, command))}
        fun build() = Router(prefix, routes, pmRoutes)
    }

    private data class Route(val path:String, val command:Command)
    private data class PrivateMessageRoute(val path:String, val command: PrivateMessageCommand)
}