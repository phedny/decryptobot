package net.phedny.decryptobot.listeners

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.phedny.decryptobot.router.Router

class EventListener(private val router: Router) : ListenerAdapter(){

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return

        println("${event.message.guild.name} : [${event.message.channel.name}] ${event.author.name}: ${event.message.contentRaw}")

        router.process(event)
    }

    override fun onPrivateMessageReceived(event: PrivateMessageReceivedEvent) {
        if (event.author.isBot) return

        println("Private Message: ${event.author.name}: ${event.message.contentRaw}")

        router.process(event)
    }
}