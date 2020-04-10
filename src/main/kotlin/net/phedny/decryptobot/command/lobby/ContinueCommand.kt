package net.phedny.decryptobot.command.lobby

import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.phedny.decryptobot.SheetsClient
import net.phedny.decryptobot.command.Command
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.GameRepository

class ContinueCommand() : Command {
    override fun execute(event: GuildMessageReceivedEvent, prefix:String) {
        val spreadsheetId = Regex("docs.google.com/spreadsheets/d/([\\w-]+)").find(event.message.contentRaw)?.groupValues?.get(1)
        if (spreadsheetId?.isNotEmpty() == true) {
            var channelMessageId: String? = null
            var setupFinished = false
            val channelMessagePrefix = "Let's continue a game for you.\n"
            event.channel.sendMessage(channelMessagePrefix + "Please give me half a minute to read the Google spreadsheet provided... :hammer_pick:")
            .queue {
                channelMessageId = it.id
                if (setupFinished) {
                    updateChannelMessage(event, it.id, channelMessagePrefix)
                }
            }

            val game = SheetsClient.readGameInfo(spreadsheetId)
            if (game == null) {
                event.channel.sendMessage("Something went wrong with reading the game. Make sure you send me the URL of a valid Decrypto spreadsheet, that the spreadsheet of the opponents still exists and that neither spreadsheets have been tampered with.")
                return
            }
            val players = game.black.players.union(game.white.players)
            val playersInGame = players.filter { player -> GameRepository.getGameByPlayerId(player).let { it != null && it != game } }
            if (playersInGame.isNotEmpty()) {
                val playersInGameStr = event.message.guild.members.filter { playersInGame.contains(it.id) }.joinToString(" ") { it.asMention }
                return event.channel.send("This group of players can't start a game, as the following players are already in an active game: $playersInGameStr")
            }

            val blackPlayers = event.message.guild.members.filter { game.black.players.contains(it.id) }
            blackPlayers.forEach {
                it.send("Welcome back to the black team. You can find the spreadsheet at https://docs.google.com/spreadsheets/d/${game.black.spreadsheetId}\n" +
                        "The four secret words for your team are: ${game.black.secretWords.joinToString()}")
            }

            val whitePlayers = event.message.guild.members.filter { game.white.players.contains(it.id) }
            whitePlayers.forEach {
                it.send("Welcome back to the white team. You can find the spreadsheet at https://docs.google.com/spreadsheets/d/${game.white.spreadsheetId}\n" +
                        "The four secret words for your team are: ${game.white.secretWords.joinToString()}.")
            }

            GameRepository.games.add(game)

            setupFinished = true
            channelMessageId?.let { updateChannelMessage(event, it, channelMessagePrefix) }
            val missingMembers = event.guild.members.filter { players.contains(it.id) }.filter { it.onlineStatus != OnlineStatus.ONLINE }
            if (missingMembers.isNotEmpty()){
                event.channel.send("Even though the game is all set up, it seems like you're missing some players: ${missingMembers.joinToString(", ") { it.asMention }}. Be sure to give them a call! :telephone_receiver:" )
            }
        }

    }
}