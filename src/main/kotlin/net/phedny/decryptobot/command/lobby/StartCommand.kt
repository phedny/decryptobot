package net.phedny.decryptobot.command.lobby

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.phedny.decryptobot.SheetsClient
import net.phedny.decryptobot.command.Command
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.GameRepository
import net.phedny.decryptobot.state.LobbyRepository
import net.phedny.decryptobot.state.Team
import net.phedny.decryptobot.state.Words

class StartCommand : Command {
    override fun execute(event: GuildMessageReceivedEvent, prefix:String) {
        val lobby = LobbyRepository.getLobby(event.guild.id, event.channel.id)
            ?: return event.channel.send("I'm not activated in this channel. Want to prepare a Decrypto team? First activate me using the `!decrypto` command :+1:")

        val playersInGame = lobby.blackPlayers.union(lobby.whitePlayers).filter { GameRepository.getGameByPlayerId(it) != null }
        if (playersInGame.isNotEmpty()) {
            val playersInGameStr =
                event.message.guild.members.filter { playersInGame.contains(it.id) }.joinToString(" ") { it.asMention }
            return event.channel.send("This group of players can't start a game, as the following players are already in an active game: $playersInGameStr")
        }

        val blackPlayers = event.message.guild.members.filter { lobby.blackPlayers.contains(it.id) }
        val whitePlayers = event.message.guild.members.filter { lobby.whitePlayers.contains(it.id) }

        val wordList = event.message.contentRaw.removePrefix(prefix).trim()
        if (wordList.isBlank()) {
            return event.channel.send("Please pick a word list to use for this game. The following word lists are available: " + Words.wordLists.joinToString(", "))
        }
        val (blackWords, whiteWords) = Words.pickWords(wordList) ?: return event.channel.send("The word list does not exist, pick one of: " + Words.wordLists.joinToString(", "))

        runBlocking {
            val channelMessagePrefix = "Let's start a game for you.\n" +
                    "Black team is formed by ${blackPlayers.joinToString { it.asMention }}\n" +
                    "White team is formed by ${whitePlayers.joinToString { it.asMention }}\n"
            var channelMessageId: String? = null
            var spreadsheetsFinished = false
            event.channel.sendMessage(channelMessagePrefix + "Please give me half a minute to pick the random keywords and prepare the Google spreadsheet... :hammer_pick:")
                .queue {
                    channelMessageId = it.id
                    if (spreadsheetsFinished) {
                        updateChannelMessage(event, it.id, channelMessagePrefix)
                    }
                }

            val (blackSpreadsheet, whiteSpreadsheet) = awaitAll(
                async { SheetsClient.initializeNewSpreadsheet() },
                async { SheetsClient.initializeNewSpreadsheet() }
            )
            val (blackSpreadsheetId, blackSheetId, blackProtectedRangeId) = blackSpreadsheet
            val (whiteSpreadsheetId, whiteSheetId, whiteProtectedRangeId) = whiteSpreadsheet

            awaitAll(
                async { SheetsClient.writeGameInfo(blackSpreadsheetId, whiteSpreadsheetId, event.guild.id, event.channel.id, "BLACK", lobby.blackPlayers, blackWords) },
                async { SheetsClient.writeGameInfo(whiteSpreadsheetId, blackSpreadsheetId, event.guild.id, event.channel.id, "WHITE", lobby.whitePlayers, whiteWords) }
            )

            blackPlayers.forEach {
                it.send("Welcome to the black team. You can find the spreadsheet at https://docs.google.com/spreadsheets/d/$blackSpreadsheetId\n" +
                        "The four secret words for your team are: ${blackWords.joinToString()}")
            }

            whitePlayers.forEach {
                it.send("Welcome to the white team. You can find the spreadsheet at https://docs.google.com/spreadsheets/d/$whiteSpreadsheetId\n" +
                        "The for secret words for your team are: ${whiteWords.joinToString()}.")
            }

            GameRepository.newGame(event.guild.id, event.channel.id, Team(blackSpreadsheetId, blackSheetId, blackProtectedRangeId, blackWords, lobby.blackPlayers, emptyList()), Team(whiteSpreadsheetId, whiteSheetId, whiteProtectedRangeId, whiteWords, lobby.whitePlayers, emptyList()))
            LobbyRepository.removeLobby(event.guild.id, event.channel.id)

            spreadsheetsFinished = true
            channelMessageId?.let { updateChannelMessage(event, it, channelMessagePrefix) }
        }
    }
}

fun updateChannelMessage(event: GuildMessageReceivedEvent, channelMessageId: String, channelMessagePrefix: String) {
    event.channel.editMessageById(channelMessageId, channelMessagePrefix +
            "Everybody should have received a link to the Google spreadsheet and the secret words.\n" +
            "If you're not sure to play the game on Discord, you can send me the `!help` command and I'll help you out. Enjoy your game!")
        .queue()
}