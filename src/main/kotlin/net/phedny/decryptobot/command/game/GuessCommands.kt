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
            otherRound.opponentGuess.any(::isNull) && !otherRound.firstRound
                -> "Thanks for the guesses. To complete the round, you also need to send in guesses for the code of the other team."
            otherRound.teamGuess.any(::isNull) || (round.opponentGuess.any(::isNull) && !round.firstRound)
                -> "Thanks for the guesses, your team has completed the round. Let's wait for the other team to finish their guesses... :stopwatch:"
            else
                -> "Thanks for the guesses. Those complete the round, so let me check everything... :face_with_monocle:"
        }
    }

    private fun processGameUpdate(event: PrivateMessageReceivedEvent, oldGame: Game, newGame: Game) {
        val oldBlackRound = oldGame.black.rounds.last()
        val oldWhiteRound = oldGame.white.rounds.last()
        val newBlackRound = newGame.black.rounds.last()
        val newWhiteRound = newGame.white.rounds.last()
        val roundFinished = newBlackRound.finished && newWhiteRound.finished

        if (roundFinished && newGame.finished) {
            GameRepository.removeGameByPlayerId(event.author.id)
        } else {
            GameRepository.updateGame(if (roundFinished) newGame.withNewRound() else newGame)
        }

        val team = newGame.getTeam(event.author.id)
        SheetsClient.writeRound(newGame, team.rounds.size)

        val guildMembers = event.author.mutualGuilds.first { it.id == newGame.guildId }.members.filter { it.id != event.author.id }
        val blackMembers = guildMembers.filter { newGame.black.players.contains(it.id) }
        val whiteMembers = guildMembers.filter { newGame.white.players.contains(it.id) }

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

        if (roundFinished) {
            val message = listOf(
                "**Round #0${team.rounds.size} has finished!**",
                "",
                "The black code was ${newBlackRound.answer.joinToString(" ")} and the black team guessed ${newBlackRound.teamGuess.joinToString(" ")}. ${if (newBlackRound.incorrectTeamGuess) "This mistake results in a black token." else "Well done!"}",
                "The white code was ${newWhiteRound.answer.joinToString(" ")} and the white team guessed ${newWhiteRound.teamGuess.joinToString(" ")}. ${if (newWhiteRound.incorrectTeamGuess) "This mistake results in a black token." else "Well done!"}",
                when {
                    newWhiteRound.firstRound -> null
                    newWhiteRound.correctOpponentGuess -> "The black team correctly guessed the white code, resulting in a white token :+1:"
                    else -> "The black team guessed ${newWhiteRound.opponentGuess.joinToString(" ")} for the white code."
                },
                when {
                    newBlackRound.firstRound -> null
                    newBlackRound.correctOpponentGuess -> "The white team correctly guessed the black code, resulting in a white token :+1:"
                    else -> "The white team guessed ${newBlackRound.opponentGuess.joinToString(" ")} for the black code."
                },
                "",
                "This results in the following token collection:",
                "- Black team has ${newGame.white.correctOpponentGuesses} white tokens and ${newGame.black.incorrectTeamGuesses} black tokens.",
                "- White team has ${newGame.black.correctOpponentGuesses} white tokens and ${newGame.white.incorrectTeamGuesses} black tokens.",
                "",
                if (newGame.finished) "This concludes the game. I hope you've enjoyed it and come back to play again :smile:" else "Let's continue to the next round."
            ).filterNotNull().joinToString("\n")

            event.author.mutualGuilds.first { it.id == newGame.guildId }.textChannels.first { it.id == newGame.channelId }.send(message)
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
            } else if (game.black.roundNumber > 1 || teamColor == TeamColor.BLACK) {
                newGame = newGame.withGuess(TeamColor.BLACK, teamColor, it)
            }
        }
        whiteRegexes.mapNotNull { regex -> regex.find(input)?.groupValues?.get(0)?.filter { "1234".contains(it) }?.map { it - '0' } }.firstOrNull()?.let {
            if (!game.white.acceptsGuesses) {
                return Pair(game, "Be a little patient, please. The white team hasn't finished their hints, yet.")
            } else if (game.white.roundNumber > 1 || teamColor == TeamColor.WHITE) {
                newGame = newGame.withGuess(TeamColor.WHITE, teamColor, it)
            }
        }

        val (newTeam, newOtherTeam) = newGame.getTeams(playerId)
        return Pair(newGame, determineResponse(newTeam, newOtherTeam))
    }
}

class GuessesReadyCommand: BaseGuessCommand() {
    override fun processGuesses(input: String, game: Game, playerId: String): Pair<Game, String> {
        val team = game.getTeam(playerId)
        val teamColor = game.getTeamColor(playerId)
        val (blackGuess, whiteGuess) = SheetsClient.readGuesses(team.spreadsheetId, team.roundNumber)

        var newGame = game
        blackGuess?.let {
            if (game.black.acceptsGuesses && (game.black.roundNumber > 1) || teamColor == TeamColor.BLACK) {
                newGame = newGame.withGuess(TeamColor.BLACK, teamColor, it)
            }
        }
        whiteGuess?.let {
            if (game.white.acceptsGuesses && (game.white.roundNumber > 1) || teamColor == TeamColor.WHITE) {
                newGame = newGame.withGuess(TeamColor.WHITE, teamColor, it)
            }
        }

        val (newTeam, newOtherTeam) = newGame.getTeams(playerId)
        return Pair(newGame, determineResponse(newTeam, newOtherTeam))
    }
}
