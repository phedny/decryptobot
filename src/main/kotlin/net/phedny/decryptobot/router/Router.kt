package net.phedny.decryptobot.router

import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.phedny.decryptobot.command.Command
import net.phedny.decryptobot.command.GuildMessageCommand
import net.phedny.decryptobot.command.PrivateMessageCommand

class Router private constructor(private val prefix:String, private val routes:List<Route<GuildMessageReceivedEvent, GuildMessageCommand>>, private val privateMessageRoutes:List<Route<PrivateMessageReceivedEvent, PrivateMessageCommand>>) {

    fun process(event: GuildMessageReceivedEvent) {
        val input = event.message.contentRaw
        process(event, input, routes)
    }

    fun process(event: PrivateMessageReceivedEvent) {
        val input = event.message.contentRaw
        process(event, input, privateMessageRoutes)
    }

    private fun <E: Event, T: Command<E>> process(event: E, input: String, routes: List<Route<E,T>>) {
        val matchingRoute = routes.firstOrNull { Regex("$prefix\\s?${it.path}.*", RegexOption.IGNORE_CASE).matches(input) } ?: return
        val inputPrefix = if (input.startsWith("$prefix ")) "$prefix ${matchingRoute.path}" else "$prefix${matchingRoute.path}"
        matchingRoute.command.execute(event, inputPrefix)
    }

    class Builder(private val prefix: String = "?") {
        private val routes = mutableListOf<Route<GuildMessageReceivedEvent, GuildMessageCommand>>()
        private val pmRoutes = mutableListOf<Route<PrivateMessageReceivedEvent, PrivateMessageCommand>>()
        fun add(path: String, command: GuildMessageCommand) = apply { routes.add(Route(path, command))}
        fun add(path: String, command: PrivateMessageCommand) = apply { pmRoutes.add(Route(path, command))}
        fun build() = Router(prefix, routes, pmRoutes)
    }

    private data class Route<E: Event, T: Command<E>> (val path:String, val command:T)
}