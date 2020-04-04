package net.phedny.decryptobot.command

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

class AnswerCommand:Command {
    override fun execute(event: GuildMessageReceivedEvent, prefix:String) {
        event.message.author.openPrivateChannel().map { it.sendMessage((1..4).shuffled().take(3).joinToString()).queue() }.queue()
    }
}