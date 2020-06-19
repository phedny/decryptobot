package net.phedny.decryptobot.command

import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent

interface Command<E: Event> {
    fun execute(event: E, prefix: String)
}

interface GuildMessageCommand : Command<GuildMessageReceivedEvent>
interface PrivateMessageCommand :Command<PrivateMessageReceivedEvent>