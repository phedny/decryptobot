package net.phedny.decryptobot.command.game

import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.phedny.decryptobot.SheetsClient
import net.phedny.decryptobot.command.PrivateMessageCommand
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.GameRepository

class HintsReadyCommand(): PrivateMessageCommand {
    override fun execute(event: PrivateMessageReceivedEvent, prefix:String) {
        val game = GameRepository.getGameByPlayerId(event.author.id)
            ?: return event.channel.send("I'm not aware of an active Decrypto game you're playing. :worried:\n" +
                    "If you are playing a game I'm unaware of, please send me a link to your spreadsheet using the `!continue` command. " +
                    "I'll take look at the spreadsheet. If it's indeed an active game, you can continue playing.")


        val (team, otherTeam) = game.getTeams(event.author.id)
        val round = team.rounds.last()

        val hints = SheetsClient.readHints(team.spreadsheetId, game.getTeamColor(event.author.id).name, team.rounds.size)

        val (newGame, message) = when {
            team.acceptsEncryptor                           -> Pair(game, "Wow, wow, not so fast. Your team has no encryptor, yet. Pick up that role by sending me the `!encrypt` command first.")
            round.encryptor != event.author.id              -> Pair(game, "You're not the encryptor for your team this round, so I can't allow you to set the hints.")
            team.acceptsHints && hints.any { it.isBlank() } -> Pair(game, "I've tried to read your hints from the spreadsheet, but it seems not all hints have been set for this round :sweat_smile:. Please check the spreadsheet or use a different way of supplying hints.")
            team.acceptsHints                               -> Pair(game.withHints(event.author.id, hints), "I've found your hints in the spreadsheet :+1:. It's time for some guess time!")
            team.acceptsGuesses                             -> Pair(game, "You've already sent me three hints, so I'm now waiting for guesses to arrive.")
            else                                            -> Pair(game, "Your team has finished this round. Please wait for your opponents to finish... :stopwatch:")
        }

        GameRepository.updateGame(newGame)
        if (game != newGame) {
            val teamColor = game.getTeamColor(event.author.id)
            SheetsClient.writeHints(team.spreadsheetId, teamColor.name, team.rounds.size, hints)
            SheetsClient.writeHints(otherTeam.spreadsheetId, teamColor.name, team.rounds.size, hints)

            val hintsAsString = hints.map { "- $it" }.joinToString("\n")
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