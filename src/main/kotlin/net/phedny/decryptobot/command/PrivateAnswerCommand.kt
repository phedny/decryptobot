package net.phedny.decryptobot.command

import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent

class PrivateAnswerCommand:PrivateMessageCommand {
    override fun execute(event: PrivateMessageReceivedEvent, prefix:String) {
        event.message.author.openPrivateChannel().map { it.sendMessage((1..4).shuffled().take(3).joinToString()).queue() }.queue()
    }
}