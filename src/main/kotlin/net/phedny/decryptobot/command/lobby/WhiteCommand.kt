package net.phedny.decryptobot.command.lobby

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.phedny.decryptobot.command.GuildMessageCommand
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.LobbyRepository

class WhiteCommand: GuildMessageCommand {
    override fun execute(event: GuildMessageReceivedEvent, prefix:String) {
        if (LobbyRepository.updateLobby(event.guild.id, event.channel.id, whitePlayers = event.message.mentionedMembers.map { it.id })) {
            event.channel.send("A white team with ${event.message.mentionedMembers.size} players, that sounds like fun! :smile:")
        } else {
            event.channel.send("I'm not activated in this channel. Want to prepare a Decrypto team? First activate me using the `!decrypto` command :+1:")
        }
    }
}