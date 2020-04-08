package net.phedny.decryptobot.command.game

import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.phedny.decryptobot.SheetsClient
import net.phedny.decryptobot.command.PrivateMessageCommand
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.GameRepository
import java.lang.IllegalStateException

class HintCommand(): PrivateMessageCommand {
    override fun execute(event: PrivateMessageReceivedEvent, prefix:String) {
        val game = GameRepository.getGameByPlayerId(event.author.id)
            ?: return event.channel.send("I'm not aware of an active Decrypto game you're playing. :worried:\n" +
                    "If you are playing a game I'm unaware of, please send me a link to your spreadsheet using the `!continue` command. " +
                    "I'll take look at the spreadsheet. If it's indeed an active game, you can continue playing.")

        val (team, otherTeam) = game.getTeams(event.author.id)
        val round = team.rounds.last()

        val suffix = event.message.contentRaw.removePrefix(prefix)
        val hintIndex = when(suffix.first()) {
            ' '         -> round.nextHintIndex()
            in '1'..'4' -> suffix.first() - '1'
            's'         -> null
            else        -> return
        }
        var newRound = round
        var newGame = game
        if (hintIndex != null && team.acceptsHints) {
            val hint = when(suffix.first()) {
                ' '         -> suffix.trim()
                in '1'..'4' -> suffix.drop(1).trim()
                else -> throw IllegalStateException()
            }
            newRound = round.withHint(hintIndex, hint)
            if (team.acceptsHints) {
                newGame = game.withHint(event.author.id, hintIndex, hint)
            }
        } else if (hintIndex == null) {
            val hints = suffix.drop(1).trim()
            val hintsList = if (hints.contains(",")) hints.split(Regex(",\\s*")) else hints.split(Regex("\\s+"))
            if (hintsList.size >= 3) {
                newRound = newRound.withHints(hintsList.take(3))
                if (team.acceptsHints) {
                    newGame = game.withHints(event.author.id, hintsList.take(3))
                }
            }
        }

        val needMoreHints = newRound.acceptsHints

        val message = when {
            team.acceptsEncryptor                   -> "Wow, wow, not so fast. Your team has no encryptor, yet. Pick up that role by sending me the `!encrypt` command first."
            round.encryptor != event.author.id      -> "You're not the encryptor for your team this round, so I can't allow you to set the hints."
            team.acceptsHints && newRound == round  -> "It seems like you're short on some hints. Either use the `!hints` command and send me three hints or use the `!hint`, `!hint1`, `!hint2`, or `!hint3` command and send me a single hint."
            team.acceptsHints && hintIndex != null  -> "Thanks for hint #${hintIndex + 1}. ${if (needMoreHints) "What's your next hint?" else "It's time for some guess work! :smile:"}"
            team.acceptsHints && hintIndex == null  -> "Thanks for the hints. ${if (needMoreHints) "What's your next hint?" else "It's time for some guess work! :smile:"}"
            team.acceptsGuesses                     -> "You've already sent me three hints, so I'm now waiting for guesses to arrive."
            else                                    -> "Your team has finished this round. Please wait for your opponents to finish... :stopwatch:"
        }

        GameRepository.updateGame(newGame)
        if (game != newGame && !needMoreHints) {
            val teamColor = game.getTeamColor(event.author.id)
            SheetsClient.writeHints(team.spreadsheetId, teamColor.name, team.rounds.size, newRound.hints)
            SheetsClient.writeHints(otherTeam.spreadsheetId, teamColor.name, team.rounds.size, newRound.hints)

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