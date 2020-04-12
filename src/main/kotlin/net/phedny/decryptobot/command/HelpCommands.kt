package net.phedny.decryptobot.command

import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.phedny.decryptobot.extensions.send

private fun sendHelp(channel: MessageChannel) {
    channel.send("**Welcome to play a game of Decrypto!** :game_die:\n\n" +
            "To start a new game, send the `!decrypto` command in any channel where I'm available and pick two teams.\n" +
            "After picking teams, you say `!start` to start the game. I will prepare a game for you:\n" +
            "1. I'll pick four random keywords for each team and distribute them to all players. Of course, you'll only learn the keywords for your own team :wink:\n" +
            "2. For each team, I'll create a spreadsheets in Google Docs and I will send every team member a link to the spreadsheet.\n" +
            "3. During the game, I'll keep track of what round the game is, I will check your guesses and distribute black and white tokens.\n" +
            "There are some things I can't do for you, so you'll have to do those yourself:\n" +
            "1. You have to come up with clever hints and make guesses. If I would do that, this wouldn't be a game for your pleasure (unless you like my humor)...\n" +
            "2. You'll have to setup a communication channel for your team. If two separate voice channels are available on this Discord server, that would be great. If not, you may ask the owner of this server to provide two channels or find a different way of communicating with your team members.\n" +
            "3. Don't cheat, that'll spoil the fun. Owh, and don't mess with the spreadsheet, I'm not as smart as you, so if you mess up the spreadsheet, I can't assist in game play.")
    channel.send("The game consists of up to 8 rounds. In each round, first one person in each team has to come up with hints to disguise a secret code and next the rest of the team must guess the correct code. Also, you have to guess the code of the other team.\n" +
            "When you're the encryptor that is going to come up with the hints, this is what you do:\n" +
            "1. Send me the `!encrypt` command in a private message. I'll provide you with a three digit secret code.\n" +
            "2. Provide the hints that you've come up with. You have four ways of doing this:\n" +
            "   a. Fill them in the spreadsheet yourself. When done, send me the `!hintsReady` command in private.\n" +
            "   b. Privately send me the three hints, by using the `!hint` command three times to provide the three hints in the order of the digits in the secret code.\n" +
            "   c. To send the hints in another order, use the `!hint1`, `!hint2` and `!hint3` commands.\n" +
            "   d. To send the hints all in one go, use the `!hints` command and send me three hints. The hints can be either comma-separated or whitespace-separated (if you only have one-word hints).")
    channel.send("3. After you sent me the `!hintsReady` command or provided three hints with methods b or c, I will communicate the hints to your team members and to the other team.\n" +
            "Next up is guessing the secret codes. Communicate with your team to figure out the codes and let me know about your guesses in one of two ways:\n" +
            "a. Fill in the guesses in the spreadsheet in the column marked with a question mark. When done, one player in your team sends me the `!guessesReady` command in private.\n" +
            "b. One player of your team sends me the guesses in private using the `!guess` command, e.g. `!guess white 1 2 3`.\n" +
            "Note that in the first round, you only provide a guess for the code of your own team.\n" +
            "When both teams have provided hints and guesses, I will check your answers and update the spreadsheets. " +
            "If your team incorrectly guessed your own code, I'll give you a black token. " +
            "If your team correctly guessed the code of the other team, I'll give you a white token. " +
            "As soon as any team has two tokens of the same color, the game will end.\n" +
            "Good luck and let's have some fun!")
}

class PrivateHelpCommand:PrivateMessageCommand {
    override fun execute(event: PrivateMessageReceivedEvent, prefix: String) {
        sendHelp(event.channel)
    }
}

class PublicHelpCommand:Command {
    override fun execute(event: GuildMessageReceivedEvent, prefix: String) {
        sendHelp(event.channel)
    }
}