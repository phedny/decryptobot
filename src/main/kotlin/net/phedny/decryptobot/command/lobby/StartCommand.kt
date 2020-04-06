package net.phedny.decryptobot.command.lobby

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.phedny.decryptobot.command.Command
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.LobbyRepository

class StartCommand: Command {
    override fun execute(event: GuildMessageReceivedEvent, prefix:String) {
        val lobby = LobbyRepository.getLobby(event.guild.id, event.channel.id)

        if (lobby == null) {
            event.channel.send("I'm not activated in this channel. Want to prepare a Decrypto team? First activate me using the `!decrypto` command :+1:")
        } else {
            val blackPlayers = event.message.guild.members.filter { lobby.blackPlayers.contains(it.id) }.map { it.asMention }
            val whitePlayers = event.message.guild.members.filter { lobby.whitePlayers.contains(it.id) }.map { it.asMention }
            event.channel.send("Let's start a game for you.\nBlack team is formed by ${blackPlayers.joinToString()}\nWhite team is formed by ${whitePlayers.joinToString()}")
            event.channel.send("Just joking... The rest of the bot is not implemented yet :sweat_smile:")
        }
    }
}