package net.phedny.decryptobot.command.game

import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.phedny.decryptobot.SheetsClient
import net.phedny.decryptobot.command.PrivateMessageCommand
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.*
import java.util.Objects.isNull

abstract class BaseGuessCommand: PrivateMessageCommand {
    override fun execute(event: PrivateMessageReceivedEvent, prefix:String) {
        val game = GameRepository.getGameByPlayerId(event.author.id)
            ?: return event.channel.send("I'm not aware of an active Decrypto game you're playing. :worried:\n" +
                    "If you are playing a game I'm unaware of, please send me a link to your spreadsheet using the `!continue` command. " +
                    "I'll take look at the spreadsheet. If it's indeed an active game, you can continue playing.")

        val (team, otherTeam) = game.getTeams(event.author.id)

        if (team.acceptsGuesses || otherTeam.acceptsGuesses) {
            val input = event.message.contentRaw.removePrefix(prefix).trim()
            val (newGame, message) = processGuesses(input, game, event.author.id)

            if (game != newGame) {
                processGameUpdate(event, game, newGame)
            }

            event.channel.send(message)
        } else {
            event.channel.send("You can't send guesses at this point in the game. I'm not able to figure out why. :sweat_smile:")
        }
    }

    fun determineResponse(team: Team, otherTeam: Team): String {
        val round = team.rounds.last()
        val otherRound = otherTeam.rounds.last()

        return when {
            round.teamGuess.any(::isNull)
                -> "Thanks for the guesses. To complete the round, you also need to send in guesses for the code of your own team."
            otherRound.opponentGuess.any(::isNull)
                -> "Thanks for the guesses. To complete the round, you also need to send in guesses for the code of the other team."
            round.opponentGuess.any(::isNull) || otherRound.teamGuess.any(::isNull)
                -> "Thanks for the guesses, your team has completed the round. Let's wait for the other team to finish their guesses... :stopwatch:"
            else
                -> "Thanks for the guesses. Those complete the round, so let me check everything... :face_with_monocle:"
        }
    }

    private fun processGameUpdate(event: PrivateMessageReceivedEvent, oldGame: Game, newGame: Game) {
        GameRepository.updateGame(newGame)
        val team = newGame.getTeam(event.author.id)

        SheetsClient.writeRound(newGame, team.rounds.size)

        val guildMembers = event.author.mutualGuilds.first { it.id == newGame.guildId }.members
        val blackMembers = guildMembers.filter { it.id != event.author.id && newGame.black.players.contains(it.id) }
        val whiteMembers = guildMembers.filter { it.id != event.author.id && newGame.white.players.contains(it.id) }

        val oldBlackRound = oldGame.black.rounds.last()
        val oldWhiteRound = oldGame.white.rounds.last()
        val newBlackRound = newGame.black.rounds.last()
        val newWhiteRound = newGame.white.rounds.last()

        if (oldBlackRound.teamGuess.any(::isNull) && newBlackRound.teamGuess.none(::isNull)) {
            blackMembers.forEach { it.send("I have received ${newBlackRound.teamGuess.joinToString(" ")} as guess for your team code.") }
        }
        if (oldWhiteRound.teamGuess.any(::isNull) && newWhiteRound.teamGuess.none(::isNull)) {
            whiteMembers.forEach { it.send("I have received ${newWhiteRound.teamGuess.joinToString(" ")} as guess for your team code.") }
        }
        if (oldWhiteRound.opponentGuess.any(::isNull) && newWhiteRound.opponentGuess.none(::isNull)) {
            blackMembers.forEach { it.send("I have received ${newWhiteRound.opponentGuess.joinToString(" ")} as guess for the code of the white team.") }
        }
        if (oldBlackRound.opponentGuess.any(::isNull) && newBlackRound.opponentGuess.none(::isNull)) {
            whiteMembers.forEach { it.send("I have received ${newBlackRound.opponentGuess.joinToString(" ")} as guess for the code of the black team.") }
        }
    }

    abstract fun processGuesses(input: String, game: Game, playerId: String): Pair<Game, String>
}

class GuessCommand: BaseGuessCommand() {
    private val BLACK = """black (?:([1234])[,\s]*){3}""".toRegex(RegexOption.IGNORE_CASE)
    private val WHITE = """white (?:([1234])[,\s]*){3}""".toRegex(RegexOption.IGNORE_CASE)
    private val OUR = """(?:our|us|we|this) (?:([1234])[,\s]*){3}""".toRegex(RegexOption.IGNORE_CASE)
    private val THEIR = """(?:their|them|they|that|other) (?:([1234])[,\s]*){3}""".toRegex(RegexOption.IGNORE_CASE)

    override fun processGuesses(input: String, game: Game, playerId: String): Pair<Game, String> {
        val teamColor = game.getTeamColor(playerId)
        val (blackRegexes, whiteRegexes) = when (teamColor) {
            TeamColor.BLACK -> Pair(listOf(BLACK, OUR), listOf(WHITE, THEIR))
            TeamColor.WHITE -> Pair(listOf(BLACK, THEIR), listOf(WHITE, OUR))
        }

        var newGame = game
        blackRegexes.mapNotNull { regex -> regex.find(input)?.groupValues?.get(0)?.filter { "1234".contains(it) }?.map { it - '0' } }.firstOrNull()?.let {
            if (!game.black.acceptsGuesses) {
                return Pair(game, "Be a little patient, please. The black team hasn't finished their hints, yet.")
            }
            newGame = newGame.withGuess(TeamColor.BLACK, teamColor, it)
        }
        whiteRegexes.mapNotNull { regex -> regex.find(input)?.groupValues?.get(0)?.filter { "1234".contains(it) }?.map { it - '0' } }.firstOrNull()?.let {
            if (!game.white.acceptsGuesses) {
                return Pair(game, "Be a little patient, please. The white team hasn't finished their hints, yet.")
            }
            newGame = newGame.withGuess(TeamColor.WHITE, teamColor, it)
        }

        val (newTeam, newOtherTeam) = newGame.getTeams(playerId)
        return Pair(newGame, determineResponse(newTeam, newOtherTeam))
    }
}

class GuessesReadyCommand: BaseGuessCommand() {
    override fun processGuesses(input: String, game: Game, playerId: String): Pair<Game, String> {
        TODO("Not yet implemented")
    }
}
