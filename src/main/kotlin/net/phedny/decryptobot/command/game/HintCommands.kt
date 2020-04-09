package net.phedny.decryptobot.command.game

import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.phedny.decryptobot.SheetsClient
import net.phedny.decryptobot.command.PrivateMessageCommand
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.Game
import net.phedny.decryptobot.state.GameRepository
import net.phedny.decryptobot.state.Round
import net.phedny.decryptobot.state.Team

abstract class BaseHintCommand: PrivateMessageCommand {
    override fun execute(event: PrivateMessageReceivedEvent, prefix:String) {
        val game = GameRepository.getGameByPlayerId(event.author.id)
            ?: return event.channel.send("I'm not aware of an active Decrypto game you're playing. :worried:\n" +
                    "If you are playing a game I'm unaware of, please send me a link to your spreadsheet using the `!continue` command. " +
                    "I'll take look at the spreadsheet. If it's indeed an active game, you can continue playing.")

        val team = game.getTeam(event.author.id)
        val round = team.rounds.last()

        if (team.acceptsHints && round.encryptor == event.author.id) {
            val input = event.message.contentRaw.removePrefix(prefix).trim()
            val (newGame, message) = processHint(input, game, team, event.author.id, round)

            if (game != newGame) {
                processGameUpdate(event, newGame)
            }

            event.channel.send(message)
        } else {
            when {
                team.acceptsEncryptor               -> "Wow, wow, not so fast. Your team has no encryptor, yet. Pick up that role by sending me the `!encrypt` command first."
                round.encryptor != event.author.id  -> "You're not the encryptor for your team this round, so I can't allow you to set the hints."
                team.acceptsGuesses                 -> "You've already sent me three hints, so I'm now waiting for guesses to arrive."
                else                                -> "Your team has finished this round. Please wait for your opponents to finish... :stopwatch:"
            }.let { event.channel.send(it) }
        }
    }

    private fun processGameUpdate(event: PrivateMessageReceivedEvent, game: Game) {
        GameRepository.updateGame(game)

        val (team, otherTeam) = game.getTeams(event.author.id)
        if (!team.acceptsHints) {
            val newRound = team.rounds.last()
            SheetsClient.writeRound(game, team.rounds.size)

            val hintsAsString = newRound.hints.map { "- $it" }.joinToString("\n")
            val guildMembers = event.author.mutualGuilds.first { it.id == game.guildId }.members
            guildMembers.filter { it.id != event.author.id && team.players.contains(it.id) }.forEach {
                it.send("Your encryptor has set the following hints for your team:\n$hintsAsString")
            }
            guildMembers.filter { otherTeam.players.contains(it.id) }.forEach {
                it.send("I have received the following hints for the other team:\n$hintsAsString")
            }
        }
    }

    abstract fun processHint(input: String, game: Game, team: Team, playerId: String, round: Round): Pair<Game, String>
}

class HintCommand: BaseHintCommand() {
    override fun processHint(input: String, game: Game, team: Team, playerId: String, round: Round): Pair<Game, String> {
        val hintIndex = round.hints.indexOfFirst { it.isNullOrBlank() }
        val newGame = game.withHint(playerId, hintIndex, input)
        return Pair(newGame, "Thanks for hint #${hintIndex + 1}. ${if (newGame.getTeam(playerId).acceptsHints) "What's your next hint?" else "It's time for some guess work! :smile:"}")
    }
}

class HintNCommand(val hintIndex: Int): BaseHintCommand() {
    override fun processHint(input: String, game: Game, team: Team, playerId: String, round: Round): Pair<Game, String> {
        val newGame = game.withHint(playerId, hintIndex, input)
        return Pair(newGame, "Thanks for hint #${hintIndex + 1}. ${if (newGame.getTeam(playerId).acceptsHints) "What's your next hint?" else "It's time for some guess work! :smile:"}")
    }
}

class HintsCommand: BaseHintCommand() {
    override fun processHint(input: String, game: Game, team: Team, playerId: String, round: Round): Pair<Game, String> {
        val hintsList = if (input.contains(",")) input.split(Regex(",\\s*")) else input.split(Regex("\\s+"))

        return when (hintsList.size) {
            in 0..2 -> Pair(game, "It seems like you're short on some hints. Either use the `!hints` command and send me three hints or use the `!hint`, `!hint1`, `!hint2`, or `!hint3` command and send me a single hint.")
            3       -> Pair(game.withHints(playerId, hintsList.take(3)), "Thanks for the hints. It's time for some guess work! :smile:")
            else    -> Pair(game, "Stop, stop, stop! You only need to provide three hints, ${hintsList.size} is a little bit too much. :sweat_smile:")
        }
    }
}

class HintsReadyCommand: BaseHintCommand() {
    override fun processHint(input: String, game: Game, team: Team, playerId: String, round: Round): Pair<Game, String> {
        val hints = SheetsClient.readHints(team.spreadsheetId, game.getTeamColor(playerId).name, team.rounds.size)
        return Pair(game.withHints(playerId, hints), "I've found your hints in the spreadsheet :+1:. It's time for some guess time!")
    }
}
