package net.phedny.decryptobot.command.lobby

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.phedny.decryptobot.command.Command
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.LobbyRepository
import net.phedny.decryptobot.state.Words

class DecryptoCommand: Command {
    override fun execute(event: GuildMessageReceivedEvent, prefix:String) {
        val members = event.channel.members.filter { !it.user.isBot }.shuffled().map { "@${it.effectiveName}" }.chunked(2)
        val wordLists = Words.wordLists

        event.channel.send("**Welcome to play a game of Decrypto!** :game_die:\n\n" +
                "Let's first pick two teams, the black team and the white team. Use the `!black` and `!white` commands to pick the teams. " +
                "After picking teams, you say `!start` to start the game with a wordlist of your choice and I will prepare a game for you. " +
                "The following word lists are available: ${wordLists.joinToString(", ")}. For example:" +
                "\n>>> !black ${members[0].joinToString(" ")}\n!white ${members[1].joinToString(" ")}\n!start ${wordLists.random()}")

        LobbyRepository.newLobby(event.guild.id, event.channel.id)
    }
}