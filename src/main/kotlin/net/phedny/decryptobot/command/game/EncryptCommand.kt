package net.phedny.decryptobot.command.game

import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.phedny.decryptobot.SheetsClient
import net.phedny.decryptobot.command.PrivateMessageCommand
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.GameRepository

class EncryptCommand(): PrivateMessageCommand {
    override fun execute(event: PrivateMessageReceivedEvent, prefix:String) {
        val game = GameRepository.getGameByPlayerId(event.author.id)
            ?: return event.channel.send("I'm not aware of an active Decrypto game you're playing. :worried:\n" +
                    "If you are playing a game I'm unaware of, please send me a link to your spreadsheet using the `!continue` command. " +
                    "I'll take look at the spreadsheet. If it's indeed an active game, you can continue playing.")

        val team = game.getTeam(event.author.id)
        val round = team.rounds.last()

        val (newGame, message) = when {
            team.acceptsEncryptor               -> Pair(game.withEncryptor(event.author.id), "Great! Are you ready to come up with some hints? The code for your team this round is: **${round.answer.joinToString(" ")}**. Good luck! :+1:")
            round.encryptor == event.author.id  -> Pair(game, "You told me to be encryptor for this round before. Did you forget the code for this round is **${round.answer.joinToString(" ")}**? Lucky for you, I remember those things :wink:")
            team.acceptsHints                   -> Pair(game, "This round already has an encryptor, I'm waiting for them to send me hints")
            team.acceptsGuesses                 -> Pair(game, "This round already has an encryptor and they have already sent me three hints. It's code guessing time!")
            else                                -> Pair(game, "Your team has finished this round. Please wait for your opponents to finish... :stopwatch:")
        }

        if (game != newGame) {
            GameRepository.updateGame(newGame)
            SheetsClient.writeEncryptorData(team.spreadsheetId, event.author.id, round.answer)
        }

        event.channel.send(message)
    }
}