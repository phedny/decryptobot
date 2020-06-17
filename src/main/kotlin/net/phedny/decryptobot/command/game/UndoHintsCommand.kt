package net.phedny.decryptobot.command.game

import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.phedny.decryptobot.SheetsClient
import net.phedny.decryptobot.command.PrivateMessageCommand
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.GameRepository
import net.phedny.decryptobot.state.TeamColor

class UndoHintsCommand: PrivateMessageCommand {
    override fun execute(event: PrivateMessageReceivedEvent, prefix:String) {
        val game = GameRepository.getGameByPlayerId(event.author.id)
            ?: return event.channel.send("I'm not aware of an active Decrypto game you're playing. :worried:\n" +
                    "If you are playing a game I'm unaware of, please send me a link to your spreadsheet using the `!continue` command. " +
                    "I'll take look at the spreadsheet. If it's indeed an active game, you can continue playing.")

        val (team, otherTeam) = game.getTeams(event.author.id)
        val teamColor = game.getTeamColor(event.author.id)
        val round = team.rounds.last()

        val (newGame, message) = when {
            team.acceptsEncryptor               -> Pair(game, "Make sure you have an encryptor for this round. Only he/she can undo hints.")
            event.author.id != round.encryptor  -> Pair(game, "You are not the encryptor of this round. Only he/she can undo hints.")
            else                                -> Pair(game.withHintsUndone(teamColor), "Any hints and guesses for the last round have been undone.")
        }

        if (game != newGame) {
            GameRepository.updateGame(newGame)
            when (teamColor){
                TeamColor.BLACK -> SheetsClient.writeRound(newGame, team.rounds.size, overrideBlack = true, overrideWhite = false)
                TeamColor.WHITE -> SheetsClient.writeRound(newGame, team.rounds.size, overrideBlack = false, overrideWhite = true)
            }
            SheetsClient.setWriteable(team.spreadsheetId, team.sheetId, team.protectedRangeId, teamColor.name, team.roundNumber, "HINTS")

            val hintsAsString = round.hints.joinToString("\n") { "- $it" }
            val guildMembers = event.author.mutualGuilds.first { it.id == game.guildId }.members
            guildMembers.filter { it.id != event.author.id && team.players.contains(it.id) }.forEach {
                it.send("Apparently your encryptor has messed up. He/she has reset the hints for your team for this round. The original hints were:\n$hintsAsString")
            }
            guildMembers.filter { otherTeam.players.contains(it.id) }.forEach {
                it.send("The other team has messed up. Their encryptor has reset the hints for their team for this round. Why don't you ask them what happened? The original hints were:\n$hintsAsString")
            }
        }

        event.channel.send(message)
    }
}