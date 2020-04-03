package net.phedny.decryptobot.command

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent

interface Command {
    fun execute(event: GuildMessageReceivedEvent, prefix:String)
}

interface PrivateMessageCommand {
    fun execute(event: PrivateMessageReceivedEvent, prefix: String)
}