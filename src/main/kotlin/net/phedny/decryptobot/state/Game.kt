package net.phedny.decryptobot.state

import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

enum class TeamColor {
    BLACK, WHITE
}

data class Game(
    val guildId: String,
    val channelId: String,
    val black: Team,
    val white: Team
) {

    fun getTeamColor(playerId: String): TeamColor = when {
        black.players.contains(playerId)    -> TeamColor.BLACK
        white.players.contains(playerId)    -> TeamColor.WHITE
        else                                -> throw IllegalArgumentException("No such player in this game")
    }

    fun getTeam(playerId: String): Team = when(getTeamColor(playerId)) {
        TeamColor.BLACK -> black
        TeamColor.WHITE -> white
    }

    fun getTeams(playerId: String): Pair<Team, Team> = when(getTeamColor(playerId)) {
        TeamColor.BLACK -> Pair(black, white)
        TeamColor.WHITE -> Pair(white, black)
    }

    fun withNewRound(): Game = copy(black = black.withNewRound(), white = white.withNewRound())
    fun withEncryptor(playerId: String): Game = when(getTeamColor(playerId)) {
        TeamColor.BLACK -> copy(black = black.withEncryptor(playerId))
        TeamColor.WHITE -> copy(white = white.withEncryptor(playerId))
    }
    fun withHint(playerId: String, hintIndex: Int, hint: String): Game = when(getTeamColor(playerId)) {
        TeamColor.BLACK -> copy(black = black.withHint(hintIndex, hint))
        TeamColor.WHITE -> copy(white = white.withHint(hintIndex, hint))
    }

    fun withHints(playerId: String, hints: List<String>): Game = when(getTeamColor(playerId)) {
        TeamColor.BLACK -> copy(black = black.withHints(hints))
        TeamColor.WHITE -> copy(white = white.withHints(hints))
    }
}

data class Team(
    val spreadsheetId: String,
    val secretWords: List<String>,
    val players: List<String>,
    val rounds: List<Round>
) {

    val acceptsEncryptor: Boolean
            get() = rounds.lastOrNull()?.acceptsEncryptor ?: false
    val acceptsHints: Boolean
            get() = rounds.lastOrNull()?.acceptsHints ?: false
    val acceptsGuesses: Boolean
            get() = rounds.lastOrNull()?.acceptsGuesses ?: false

    fun withNewRound(): Team {
        if (rounds.lastOrNull()?.finished == false) {
            throw IllegalStateException("Can't progress to new round when the last round is not yet finished")
        }

        return copy(rounds = rounds.plus(Round()))
    }

    fun withEncryptor(playerId: String): Team = copy(rounds = rounds.dropLast(1).plus(rounds.last().withEncryptor(playerId)))
    fun withHint(hintIndex: Int, hint: String): Team = copy(rounds = rounds.dropLast(1).plus(rounds.last().withHint(hintIndex, hint)))
    fun withHints(hints: List<String>): Team = copy(rounds = rounds.dropLast(1).plus(rounds.last().withHints(hints)))
}

data class Round(
    val answer: List<Int> = (1..4).shuffled().take(3),
    val encryptor: String? = null,
    val hints: List<String?> = listOf(null, null, null),
    val teamGuesses: List<Int?> = listOf(null, null, null),
    val opponentGuesses: List<Int?> = listOf(null, null, null)
) {

    val acceptsEncryptor: Boolean
            get() = encryptor == null
    val acceptsHints: Boolean
            get() = hints.any { it == null }
    val acceptsGuesses: Boolean
            get() = hints.none { it == null } && (teamGuesses.any { it == null } || opponentGuesses.any { it == null })
    val finished: Boolean
            get() = !acceptsHints && !acceptsGuesses

    fun nextHintIndex() = hints.indexOfFirst { it.isNullOrBlank() }

    fun withEncryptor(playerId: String): Round {
        if (!acceptsEncryptor) {
            throw IllegalStateException("Round does not accept an encryptor")
        }

        return copy(encryptor = playerId)
    }

    fun withHint(hintIndex: Int, hint: String): Round {
        if (!acceptsHints) {
            throw IllegalStateException("Round does not accept hints")
        }

        return copy(hints = hints.mapIndexed { i, s -> if (i == hintIndex) hint else s })
    }

    fun withHints(hints: List<String>): Round {
        if (!acceptsHints) {
            throw IllegalStateException("Round does not accept hints")
        }
        if (hints.size != 3) {
            throw IllegalArgumentException("Must have exactly three hints")
        }

        return copy(hints = hints)
    }
}

object GameRepository {
    val games: MutableList<Game> = mutableListOf()

    fun getGameByPlayerId(playerId: String): Game? = games.find { it.black.players.contains(playerId) || it.white.players.contains(playerId) }

    fun updateGame(newGame: Game) {
        val gameIndex = games.indexOfFirst { it.white.spreadsheetId == newGame.white.spreadsheetId }
        if (gameIndex == -1) {
            throw IllegalArgumentException("Cannot update unknown game")
        }

        games[gameIndex] = newGame
    }

    fun newGame(guildId: String, channelId: String, black: Team, white: Team) {
        games.add(Game(guildId, channelId, black, white).withNewRound())
    }
}
