package net.phedny.decryptobot.command.lobby

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.phedny.decryptobot.command.Command
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.LobbyRepository

class DecryptoCommand: Command {
    override fun execute(event: GuildMessageReceivedEvent, prefix:String) {
        val members = event.channel.members.filter { !it.user.isBot }.shuffled().map { it.asMention }.chunked(2)

        event.channel.send("**Welcome to play a game of Decrypto!** :game_die:\n\n" +
                "Let's first pick two teams, the black team and the white team. Use the `!black` and `!white` commands to pick the teams. " +
                "After picking teams, you say `!start` to start the game and I will prepare a game for you. For example:" +
                "\n>>> !black ${members[0].joinToString(" ")}\n!white ${members[1].joinToString(" ")}\n!start")

        LobbyRepository.newLobby(event.guild.id, event.channel.id)
    }
}