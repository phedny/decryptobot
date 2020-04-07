package net.phedny.decryptobot.command.lobby

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.phedny.decryptobot.SheetsClient
import net.phedny.decryptobot.command.Command
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.LobbyRepository
import net.phedny.decryptobot.state.Words

class StartCommand(private val sheetsClient: SheetsClient) : Command {
    override fun execute(event: GuildMessageReceivedEvent, prefix:String) {
        val lobby = LobbyRepository.getLobby(event.guild.id, event.channel.id)

        if (lobby == null) {
            event.channel.send("I'm not activated in this channel. Want to prepare a Decrypto team? First activate me using the `!decrypto` command :+1:")
        } else {
            val blackPlayers = event.message.guild.members.filter { lobby.blackPlayers.contains(it.id) }
            val whitePlayers = event.message.guild.members.filter { lobby.whitePlayers.contains(it.id) }

            val channelMessagePrefix = "Let's start a game for you.\n" +
                    "Black team is formed by ${blackPlayers.map { it.asMention }.joinToString()}\n" +
                    "White team is formed by ${whitePlayers.map { it.asMention }.joinToString()}\n"
            var channelMessageId: String? = null
            var spreadsheetsFinished: Boolean = false
            event.channel.sendMessage(channelMessagePrefix + "Please give me a moment to pick the random keywords and prepare the Google spreadsheet... :hammer_pick:")
                .queue {
                    channelMessageId = it.id
                    if (spreadsheetsFinished) {
                        updateChannelMessage(event, it.id, channelMessagePrefix)
                    }
                }

            val (blackWords, whiteWords) = Words.pickWords()
            val blackSpreadsheetId = sheetsClient.initializeNewSpreadsheet()
            val whiteSpreadsheetId = sheetsClient.initializeNewSpreadsheet()

            sheetsClient.writeGameInfo(blackSpreadsheetId, whiteSpreadsheetId, event.guild.id, event.channel.id, "BLACK", lobby.blackPlayers, blackWords)
            sheetsClient.writeGameInfo(whiteSpreadsheetId, blackSpreadsheetId, event.guild.id, event.channel.id, "WHITE", lobby.whitePlayers, whiteWords)

            blackPlayers.forEach {
                it.send("Welcome to the black team. You can find the spreadsheet at https://docs.google.com/spreadsheets/d/$blackSpreadsheetId\n" +
                        "The for secret words for your team are: ${blackWords.joinToString()}")
            }

            whitePlayers.forEach {
                it.send("Welcome to the white team. You can find the spreadsheet at https://docs.google.com/spreadsheets/d/$whiteSpreadsheetId\n" +
                        "The for secret words for your team are: ${whiteWords.joinToString()}.\n" +
                        "If you're not sure how to play the game on Discord, you can send me the `!help` command and I'll help you out. Enjoy your game!")
            }

            spreadsheetsFinished = true
            if (channelMessageId != null) {
                updateChannelMessage(event, channelMessageId!!, channelMessagePrefix)
            }
        }
    }

    private fun updateChannelMessage(event: GuildMessageReceivedEvent, channelMessageId: String, channelMessagePrefix: String) {
        event.channel.editMessageById(channelMessageId, channelMessagePrefix +
                "Everybody should have received a link to the Google spreadsheet and the secret words.\n" +
                "If you're not sure to play the game on Discord, you can send me the `!help` command and I'll help you out. Enjoy your game!")
            .queue()
    }
}