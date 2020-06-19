package net.phedny.decryptobot.command.lobby

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.phedny.decryptobot.command.GuildMessageCommand
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.LobbyRepository

class BlackCommand: GuildMessageCommand {
    override fun execute(event: GuildMessageReceivedEvent, prefix:String) {
        if (LobbyRepository.updateLobby(event.guild.id, event.channel.id, blackPlayers = event.message.mentionedMembers.map { it.id })) {
            event.channel.send("A black team with ${event.message.mentionedMembers.size} players, that's going to work! :+1:")
        } else {
            event.channel.send("I'm not activated in this channel. Want to prepare a Decrypto team? First activate me using the `!decrypto` command :+1:")
        }
    }
}