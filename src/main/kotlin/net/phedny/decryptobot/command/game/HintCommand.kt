package net.phedny.decryptobot.command.game

import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.phedny.decryptobot.SheetsClient
import net.phedny.decryptobot.command.PrivateMessageCommand
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.GameRepository

class HintCommand(private val sheetsClient: SheetsClient): PrivateMessageCommand {
    override fun execute(event: PrivateMessageReceivedEvent, prefix:String) {
        val game = GameRepository.getGameByPlayerId(event.author.id)
            ?: return event.channel.send("I'm not aware of an active Decrypto game you're playing. :worried:\n" +
                    "If you are playing a game I'm unaware of, please send me a link to your spreadsheet using the `!continue` command. " +
                    "I'll take look at the spreadsheet. If it's indeed an active game, you can continue playing.")

        val (team, otherTeam) = game.getTeams(event.author.id)
        val round = team.rounds.last()

        val suffix = event.message.contentRaw.removePrefix(prefix)
        val (hintIndex, hint) = when(suffix.first()) {
            ' '         -> Pair(round.hints.indexOfFirst { it.isNullOrBlank() }, suffix.trim())
            in '1'..'4' -> Pair(suffix.first() - '1', suffix.drop(1).trim())
            else        -> return
        }
        val newRound = round.withHint(hintIndex, hint)
        val needMoreHints = newRound.acceptsHints

        val (newGame, message) = when {
            team.acceptsEncryptor               -> Pair(game, "Wow, wow, not so fast. Your team has no encryptor, yet. Pick up that role by sending me the `!encrypt` command first.")
            round.encryptor != event.author.id  -> Pair(game, "You're not the encryptor for your team this round, so I can't allow you to set the hints.")
            team.acceptsHints                   -> Pair(game.withHint(event.author.id, hintIndex, hint), "Thanks for hint #${hintIndex + 1}. ${if (needMoreHints) "What's your next hint?" else "It's time for some guess work! :smile:"}")
            team.acceptsGuesses                 -> Pair(game, "You've already sent me three hints, so I'm now waiting for guesses to arrive.")
            else                                -> Pair(game, "Your team has finished this round. Please wait for your opponents to finish... :stopwatch:")
        }

        GameRepository.updateGame(newGame)
        if (game != newGame && !needMoreHints) {
            val teamColor = game.getTeamColor(event.author.id)
            sheetsClient.writeHints(team.spreadsheetId, teamColor.name, team.rounds.size, newRound.hints)
            sheetsClient.writeHints(otherTeam.spreadsheetId, teamColor.name, team.rounds.size, newRound.hints)

            val hintsAsString = newRound.hints.map { "- $it" }.joinToString("\n")
            val guildMembers = event.author.mutualGuilds.first { it.id == game.guildId }.members
            guildMembers.filter { it.id != event.author.id && team.players.contains(it.id) }.forEach {
                it.send("Your encryptor has set the following hints for your team:\n$hintsAsString")
            }
            guildMembers.filter { otherTeam.players.contains(it.id) }.forEach {
                it.send("I have received the following hints for the other team:\n$hintsAsString")
            }
        }

        event.channel.send(message)
    }
}